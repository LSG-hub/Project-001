"""What the calls module needs from outside. The container plugs in Gemini Live,
Gemini for summaries, and a channel to the caller (the browser test page today,
the phone line later)."""

from collections.abc import AsyncIterator, Sequence
from contextlib import AbstractAsyncContextManager
from dataclasses import dataclass
from datetime import datetime
from typing import Any, Protocol

from pydantic import BaseModel

from project001_api.core.voice import LiveEvent, Tool


class VoiceSession(Protocol):
    async def send_audio(self, pcm16k: bytes) -> None: ...

    async def send_text(self, text: str) -> None: ...

    async def send_tool_result(self, call_id: str, name: str, result: dict[str, Any]) -> None: ...

    async def send_note(self, text: str) -> None:
        """Add a silent note to the conversation without asking for a reply."""
        ...

    def events(self) -> AsyncIterator[LiveEvent]: ...


class VoiceAgent(Protocol):
    def connect(
        self, instructions: str, tools: list[Tool], resume_handle: str | None = None, voice: str | None = None
    ) -> AbstractAsyncContextManager[VoiceSession]: ...


class ExpectedCallInfo(Protocol):
    @property
    def id(self) -> str: ...

    @property
    def text(self) -> str: ...

    @property
    def expires_at(self) -> datetime: ...


class OwnerInfo(Protocol):
    @property
    def name(self) -> str | None: ...

    @property
    def about(self) -> str: ...

    @property
    def timezone(self) -> str: ...

    @property
    def voice(self) -> str | None: ...

    @property
    def expected(self) -> Sequence[ExpectedCallInfo]: ...


class OwnerContextSource(Protocol):
    """The owner's standing context (the context module)."""

    async def owner_info(self, owner_id: str) -> OwnerInfo: ...

    async def expectation_met(self, owner_id: str, expected_id: str, call_id: str) -> None: ...


class StructuredModel(Protocol):
    async def generate[T: BaseModel](self, instructions: str, content: str, schema: type[T]) -> T: ...


# What the caller's side sends us.
@dataclass(frozen=True)
class CallerAudio:
    pcm16k: bytes


@dataclass(frozen=True)
class CallerText:
    """Typed instead of spoken (test page): handy without a microphone."""

    text: str


@dataclass(frozen=True)
class CallerHangup:
    pass


type CallerMessage = CallerAudio | CallerText | CallerHangup


class CallChannel(Protocol):
    """The caller's end of the line."""

    async def receive(self) -> CallerMessage | None:
        """The next message, or None once the caller is gone."""
        ...

    async def send_audio(self, pcm24k: bytes) -> None: ...

    async def send_event(self, event: dict[str, Any]) -> None: ...
