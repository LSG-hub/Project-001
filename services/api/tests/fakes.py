"""Stand-ins for Gemini, so call tests run offline and instantly."""

import asyncio
import re
from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from pydantic import BaseModel

from project001_api.core.voice import EventKind, LiveEvent, Tool
from project001_api.modules.calls.models import CallSummary, Category, ScamRisk


class FakeVoiceSession:
    """Greets on connect, answers each caller line, and hangs up when the caller says 'OTP'."""

    def __init__(self, instructions: str = "") -> None:
        self.instructions = instructions
        self.queue: asyncio.Queue[LiveEvent | None] = asyncio.Queue()
        self.audio_in = 0
        self.tool_results: list[tuple[str, dict[str, Any]]] = []
        self.notes: list[str] = []

    def _say(self, *fragments: str) -> None:
        self.queue.put_nowait(LiveEvent(EventKind.AUDIO, audio=b"\x00\x00" * 240))
        for f in fragments:
            self.queue.put_nowait(LiveEvent(EventKind.ASSISTANT_SAID, text=f))
        self.queue.put_nowait(LiveEvent(EventKind.TURN_COMPLETE))

    async def send_text(self, text: str) -> None:
        if text.startswith("Perform this script"):
            self._say("Hello, this is", " Sam's assistant...")
        elif text.startswith("(The call has just connected"):
            self._say("Hello, this is", " Sam's assistant.", " Who's calling?")
        elif "last time" in text:
            past = re.findall(r"\[(c_[0-9a-f]+)\]", self.instructions)
            self.queue.put_nowait(
                LiveEvent(
                    EventKind.TOOL_CALL,
                    call_id="t2",
                    name="read_past_call",
                    args={"call_id": past[0] if past else "c_none"},
                )
            )
        elif "OTP" in text:
            self._say("I can't share", " any codes. Goodbye.")
            self.queue.put_nowait(
                LiveEvent(EventKind.TOOL_CALL, call_id="t1", name="end_call", args={"reason": "asked for OTP"})
            )
        else:
            self._say("Thanks,", " I'll pass that on.")

    async def send_audio(self, pcm16k: bytes) -> None:
        self.audio_in += len(pcm16k)
        if self.audio_in == len(pcm16k):  # first chunk: pretend we heard a sentence
            self.queue.put_nowait(LiveEvent(EventKind.CALLER_SAID, text="This is"))
            self.queue.put_nowait(LiveEvent(EventKind.CALLER_SAID, text=" the courier."))
            self._say("Okay,", " please leave it at the gate.")

    async def send_note(self, text: str) -> None:
        self.notes.append(text)

    async def send_tool_result(self, call_id: str, name: str, result: dict[str, Any]) -> None:
        self.tool_results.append((name, result))
        if name == "read_past_call":
            self._say("Yes,", " you said you'd call back.")
            return
        # A real model says nothing more here; end of turn after the tool result.
        self.queue.put_nowait(LiveEvent(EventKind.TURN_COMPLETE))

    async def events(self) -> AsyncIterator[LiveEvent]:
        while (event := await self.queue.get()) is not None:
            yield event


class FakeVoiceAgent:
    def __init__(self) -> None:
        self.sessions: list[FakeVoiceSession] = []
        self.instructions: list[str] = []
        self.tools: list[list[Tool]] = []
        self.voices: list[str | None] = []

    @asynccontextmanager
    async def connect(
        self, instructions: str, tools: list[Tool], resume_handle: str | None = None, voice: str | None = None
    ) -> AsyncIterator[FakeVoiceSession]:
        self.voices.append(voice)
        session = FakeVoiceSession(instructions)
        self.sessions.append(session)
        self.instructions.append(instructions)
        self.tools.append(tools)
        yield session


class FakeStructuredModel:
    def __init__(self) -> None:
        self.prompts: list[str] = []

    async def generate[T: BaseModel](self, instructions: str, content: str, schema: type[T]) -> T:
        self.prompts.append(content)
        scam = "OTP" in content
        expected = re.findall(r"\[(e_[0-9a-f]+)\]", content) if "Amazon" in content else []
        summary = CallSummary(
            summary="Someone asked for an OTP." if scam else "A courier called about a parcel.",
            caller=None,
            purpose="OTP request" if scam else "Delivery",
            category=Category.SCAM if scam else Category.DELIVERY,
            scam_risk=ScamRisk.HIGH if scam else ScamRisk.NONE,
            scam_signals=["asked for an OTP"] if scam else [],
            action_items=[],
            language="English",
            fulfilled_expected_calls=expected,
        )
        return schema.model_validate(summary.model_dump())
