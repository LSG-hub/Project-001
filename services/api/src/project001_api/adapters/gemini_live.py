"""Gemini Live on Vertex AI: a typed layer over the SDK's live session.

You send 16 kHz PCM audio (or text) in and get `LiveEvent`s out: the assistant's
24 kHz audio, transcripts of both sides, interruptions, end of turn, tool calls and
resumption handles. A Live connection lasts about 10 minutes, so a long call
reconnects with the latest handle (`resume_handle`) and keeps its context.
"""

from collections.abc import AsyncIterator
from contextlib import asynccontextmanager
from typing import Any

from google import genai
from google.genai import types

from project001_api.adapters.vertex import LazyGenaiClient
from project001_api.core.voice import INPUT_RATE, EventKind, LiveEvent, Tool


class GeminiLiveSession:
    def __init__(self, session: genai.live.AsyncSession) -> None:
        self._session = session

    async def send_audio(self, pcm16k: bytes) -> None:
        await self._session.send_realtime_input(audio=types.Blob(data=pcm16k, mime_type=f"audio/pcm;rate={INPUT_RATE}"))

    async def send_text(self, text: str) -> None:
        await self._session.send_realtime_input(text=text)

    async def send_note(self, text: str) -> None:
        # Added to the context in order, with no turn end, so the model doesn't reply to it.
        await self._session.send_client_content(
            turns=types.Content(role="user", parts=[types.Part(text=text)]), turn_complete=False
        )

    async def send_tool_result(self, call_id: str, name: str, result: dict[str, Any]) -> None:
        await self._session.send_tool_response(
            function_responses=types.FunctionResponse(id=call_id, name=name, response=result)
        )

    async def events(self) -> AsyncIterator[LiveEvent]:
        """Every event until the connection closes (the SDK's receive() stops after each turn)."""
        while True:
            got_any = False
            async for message in self._session.receive():
                got_any = True
                for event in to_events(message):
                    yield event
            if not got_any:
                return


def to_events(message: types.LiveServerMessage) -> list[LiveEvent]:
    events: list[LiveEvent] = []
    content = message.server_content
    if content:
        if content.interrupted:
            events.append(LiveEvent(EventKind.INTERRUPTED))
        for part in (content.model_turn.parts or []) if content.model_turn else []:
            if part.inline_data and part.inline_data.data:
                events.append(LiveEvent(EventKind.AUDIO, audio=part.inline_data.data))
        if content.input_transcription and content.input_transcription.text:
            events.append(LiveEvent(EventKind.CALLER_SAID, text=content.input_transcription.text))
        if content.output_transcription and content.output_transcription.text:
            events.append(LiveEvent(EventKind.ASSISTANT_SAID, text=content.output_transcription.text))
        if content.turn_complete:
            events.append(LiveEvent(EventKind.TURN_COMPLETE))
    if message.tool_call:
        for call in message.tool_call.function_calls or []:
            events.append(LiveEvent(EventKind.TOOL_CALL, call_id=call.id, name=call.name, args=dict(call.args or {})))
    update = message.session_resumption_update
    if update and update.resumable and update.new_handle:
        events.append(LiveEvent(EventKind.RESUME_HANDLE, text=update.new_handle))
    if message.go_away:
        events.append(LiveEvent(EventKind.GOING_AWAY))
    return events


class GeminiLive:
    def __init__(self, client: LazyGenaiClient, model: str, voice: str | None = None) -> None:
        self._client = client
        self._model = model
        self._voice = voice

    def config(
        self, instructions: str, tools: list[Tool], resume_handle: str | None = None, voice: str | None = None
    ) -> types.LiveConnectConfig:
        voice_name = voice or self._voice
        speech = (
            types.SpeechConfig(
                voice_config=types.VoiceConfig(prebuilt_voice_config=types.PrebuiltVoiceConfig(voice_name=voice_name))
            )
            if voice_name
            else None
        )
        declarations = [
            types.FunctionDeclaration(name=t.name, description=t.description, parameters_json_schema=t.parameters)
            for t in tools
        ]
        return types.LiveConnectConfig(
            response_modalities=[types.Modality.AUDIO],
            system_instruction=instructions,
            speech_config=speech,
            tools=[types.Tool(function_declarations=declarations)] if declarations else None,
            input_audio_transcription=types.AudioTranscriptionConfig(),
            output_audio_transcription=types.AudioTranscriptionConfig(),
            session_resumption=types.SessionResumptionConfig(handle=resume_handle),
            context_window_compression=types.ContextWindowCompressionConfig(sliding_window=types.SlidingWindow()),
        )

    @asynccontextmanager
    async def connect(
        self, instructions: str, tools: list[Tool], resume_handle: str | None = None, voice: str | None = None
    ) -> AsyncIterator[GeminiLiveSession]:
        config = self.config(instructions, tools, resume_handle, voice)
        async with self._client.get().aio.live.connect(model=self._model, config=config) as session:
            yield GeminiLiveSession(session)
