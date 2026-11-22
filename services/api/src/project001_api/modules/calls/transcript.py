"""Builds a clean transcript from the stream of transcription fragments.

Gemini Live sends transcripts in small pieces ("Hello, this", " is Sam's", " assistant.")
for both sides. Consecutive pieces from one speaker form a turn; a new speaker, the end
of the assistant's turn, or an interruption starts the next one.
"""

from collections.abc import Callable
from dataclasses import dataclass, field

from project001_api.modules.calls.models import Speaker, Turn


@dataclass
class _Open:
    speaker: Speaker
    at: float
    parts: list[str] = field(default_factory=list)
    interrupted: bool = False


class TranscriptBuilder:
    def __init__(self, elapsed: Callable[[], float]) -> None:
        self._elapsed = elapsed
        self._done: list[_Open] = []
        self._current: _Open | None = None

    def add(self, speaker: Speaker, fragment: str) -> None:
        if self._current is None or self._current.speaker is not speaker:
            self._close()
            self._current = _Open(speaker, round(self._elapsed(), 1))
        self._current.parts.append(fragment)

    def end_of_turn(self) -> None:
        """The assistant finished speaking."""
        if self._current and self._current.speaker is Speaker.ASSISTANT:
            self._close()

    def interrupted(self) -> None:
        """The caller talked over the assistant."""
        if self._current and self._current.speaker is Speaker.ASSISTANT:
            self._current.interrupted = True
            self._close()
        elif self._done and self._done[-1].speaker is Speaker.ASSISTANT:
            self._done[-1].interrupted = True

    def turns(self) -> list[Turn]:
        self._close()
        turns = []
        for t in self._done:
            text = " ".join("".join(t.parts).split())
            if text:
                turns.append(Turn(speaker=t.speaker, text=text, at=t.at, interrupted=t.interrupted))
        return turns

    def _close(self) -> None:
        if self._current is not None:
            self._done.append(self._current)
            self._current = None
