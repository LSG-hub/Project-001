"""Test calls over a WebSocket, plus the finished calls.

Everything here is a developer tool for now: on only where `dev_tools` is enabled
(local by default), because it's unauthenticated and spends Gemini credits.

WebSocket protocol (`/v1/calls/test?caller=+91...&voice=Kore`):
- you send: binary frames of 16 kHz 16-bit mono PCM (the caller's voice);
  {"type": "text", "text": "..."} to type instead of speak; {"type": "end"} to hang up.
- you get: binary frames of 24 kHz 16-bit mono PCM (the assistant's voice), and JSON
  events: call_started, caller_said, assistant_said, interrupted, turn_complete,
  tool_call, going_away, call_ending, error, and finally call_ended with the full
  call (transcript and summary).
"""

import contextlib
import io
import json
import wave
from importlib.resources import files
from typing import Annotated, Any

from fastapi import APIRouter, Depends, Query, WebSocket, WebSocketDisconnect
from fastapi.responses import HTMLResponse, Response
from pydantic import BaseModel

from project001_api.core.errors import AppError, Unauthorized
from project001_api.core.phone import to_e164
from project001_api.core.security import CurrentUid, TokenVerifier
from project001_api.core.voice import OUTPUT_RATE
from project001_api.modules.calls.models import Call
from project001_api.modules.calls.ports import CallerAudio, CallerHangup, CallerMessage, CallerText
from project001_api.modules.calls.samples import VoiceSamples
from project001_api.modules.calls.service import CallService


class WebSocketChannel:
    """The caller's end of the line, over a WebSocket."""

    def __init__(self, ws: WebSocket) -> None:
        self._ws = ws
        self._open = True

    async def receive(self) -> CallerMessage | None:
        while self._open:
            message = await self._ws.receive()
            if message["type"] == "websocket.disconnect":
                self._open = False
                return None
            if (data := message.get("bytes")) is not None:
                return CallerAudio(data)
            try:
                payload = json.loads(message.get("text") or "")
            except json.JSONDecodeError:
                continue
            if payload.get("type") == "end":
                return CallerHangup()
            if payload.get("type") == "text" and str(payload.get("text", "")).strip():
                return CallerText(str(payload["text"]).strip()[:500])
        return None

    async def send_audio(self, pcm24k: bytes) -> None:
        if self._open:
            try:
                await self._ws.send_bytes(pcm24k)
            except (WebSocketDisconnect, RuntimeError):
                self._open = False

    async def send_event(self, event: dict[str, Any]) -> None:
        if self._open:
            try:
                await self._ws.send_json(event)
            except (WebSocketDisconnect, RuntimeError):
                self._open = False

    async def close(self) -> None:
        if self._open:
            self._open = False
            with contextlib.suppress(Exception):
                await self._ws.close()


class VoiceOut(BaseModel):
    name: str
    style: str
    gender: str
    default: bool = False


class LanguageOut(BaseModel):
    code: str
    label: str


class CallContextOut(BaseModel):
    """Exactly what the assistant would start a call with."""

    caller_number: str | None
    instructions: str
    tools: list[str]


def _wav(pcm: bytes, rate: int) -> bytes:
    buffer = io.BytesIO()
    with wave.open(buffer, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(rate)
        w.writeframes(pcm)
    return buffer.getvalue()


def _caller(raw: str | None) -> str | None:
    """A caller number as E.164 (Indian numbers may skip +91); None when withheld."""
    raw = (raw or "").strip()
    return to_e164(raw, "IN", mobile_only=False) if raw else None


def build_router(
    service: CallService,
    samples: VoiceSamples,
    current_owner: CurrentUid,
    verify: TokenVerifier,
    *,
    test_calls: bool,
    dev_tools: bool,
    dev_owner: str | None,
) -> APIRouter:
    router = APIRouter(tags=["calls"])
    Owner = Annotated[str, Depends(current_owner)]

    @router.get("/v1/voices")
    async def voices() -> list[VoiceOut]:
        """The assistant's voices (Gemini Live prebuilt voices)."""
        return [
            VoiceOut(name=v.name, style=v.style, gender=v.gender, default=v.name == samples.default_voice)
            for v in samples.voices()
        ]

    @router.get("/v1/calls")
    async def recent_calls(owner: Owner, limit: int = Query(default=20, ge=1, le=100)) -> list[Call]:
        """Your recent calls, newest first, with transcripts and summaries."""
        return await service.recent(owner, limit)

    @router.get("/v1/calls/context")
    async def call_context(owner: Owner, caller: str | None = Query(default=None, max_length=24)) -> CallContextOut:
        """What the assistant would know if `caller` rang now: the exact instructions it gets."""
        number = _caller(caller)
        setup = await service.prepare(owner, number)
        return CallContextOut(
            caller_number=number, instructions=setup.instructions, tools=[t.name for t in setup.tools]
        )

    @router.get("/v1/calls/{call_id}")
    async def get_call(owner: Owner, call_id: str) -> Call:
        return await service.get(owner, call_id)

    if test_calls:

        @router.websocket("/v1/calls/test")
        async def test_call(
            ws: WebSocket,
            owner: str | None = Query(default=None, max_length=40),
            caller: str | None = Query(default=None, max_length=24),
            voice: str | None = Query(default=None, max_length=40),
        ) -> None:
            """A test call to your own assistant. Sign in with an `Authorization: Bearer` header
            (in a header, not the URL, so tokens never reach request logs). Locally, without a
            token, it's the dev owner's assistant. `owner` is a fallback name."""
            await ws.accept()
            channel = WebSocketChannel(ws)
            auth = ws.headers.get("authorization") or ""
            try:
                if auth.startswith("Bearer "):
                    owner_id = await verify(auth.removeprefix("Bearer ").strip())
                elif dev_owner:
                    owner_id = dev_owner
                else:
                    raise Unauthorized("missing_token", "Sign in first.")
                chosen = samples.check_voice(voice)
                number = _caller(caller)
            except AppError as e:
                await channel.send_event({"type": "error", "message": e.message})
                await channel.close()
                return
            call = await service.run(
                channel,
                owner_id=owner_id,
                caller_number=number,
                source="test",
                voice=chosen,
                owner_name=(owner or "").strip() or None,
            )
            await channel.send_event({"type": "call_ended", "call": call.model_dump(mode="json")})
            await channel.close()

    if not (dev_tools and dev_owner):
        return router

    @router.get("/v1/voices/sample-languages")
    async def sample_languages() -> list[LanguageOut]:
        """Languages the voice sample script is written in."""
        return [LanguageOut(code=lang.code, label=lang.label) for lang in samples.sample_languages()]

    @router.get("/v1/voices/sample-script")
    async def sample_script(
        owner: str | None = Query(default=None, max_length=40),
        lang: str = Query(default="en", max_length=8),
        voice: str | None = Query(default=None, max_length=40),
    ) -> dict[str, str]:
        """The script every voice sample reads, so voices can be compared on the same words."""
        return {"script": samples.sample_script((owner or "").strip() or None, lang, voice)}

    @router.get("/v1/voices/{name}/sample", responses={200: {"content": {"audio/wav": {}}}})
    async def voice_sample(
        name: str,
        owner: str | None = Query(default=None, max_length=40),
        lang: str = Query(default="en", max_length=8),
    ) -> Response:
        """About 30 s of the voice reading the sample script. The first request records it (~30 s)."""
        sample = await samples.voice_sample(name, (owner or "").strip() or None, lang)
        return Response(_wav(sample.pcm24k, OUTPUT_RATE), media_type="audio/wav")

    @router.get("/dev/call", include_in_schema=False)
    async def test_call_page() -> HTMLResponse:
        page = files("project001_api.modules.calls").joinpath("static/test_call.html").read_text("utf-8")
        return HTMLResponse(page)

    return router
