"""The shared vocabulary of a live voice conversation.

The Gemini Live adapter produces these events and the calls module consumes them,
without either importing the other.
"""

from dataclasses import dataclass, field
from enum import StrEnum
from typing import Any

# Audio is 16-bit little-endian mono PCM.
INPUT_RATE = 16_000  # caller -> assistant
OUTPUT_RATE = 24_000  # assistant -> caller


class EventKind(StrEnum):
    AUDIO = "audio"  # a chunk of the assistant's voice
    CALLER_SAID = "caller_said"  # transcript fragment of the caller
    ASSISTANT_SAID = "assistant_said"  # transcript fragment of the assistant
    INTERRUPTED = "interrupted"  # the caller talked over the assistant; stop playback
    TURN_COMPLETE = "turn_complete"  # the assistant finished speaking
    TOOL_CALL = "tool_call"  # the assistant wants us to run a function
    RESUME_HANDLE = "resume_handle"  # reconnect with this to keep the conversation
    GOING_AWAY = "going_away"  # the connection will close soon (Live sessions last ~10 min)


@dataclass(frozen=True)
class LiveEvent:
    kind: EventKind
    audio: bytes | None = None
    text: str | None = None
    # TOOL_CALL only
    call_id: str | None = None
    name: str | None = None
    args: dict[str, Any] = field(default_factory=dict)


@dataclass(frozen=True)
class Voice:
    name: str
    style: str
    gender: str  # "female" or "male"


# Gemini Live's 30 prebuilt voices. Style words are from the Live API docs; genders are
# from Google's Gemini-TTS docs (same voice set). All 30 checked on gemini-3.8-live.
VOICES: dict[str, Voice] = {
    v.name: v
    for v in [
        Voice("Achernar", "Soft", "female"),
        Voice("Achird", "Friendly", "male"),
        Voice("Algenib", "Gravelly", "male"),
        Voice("Algieba", "Smooth", "male"),
        Voice("Alnilam", "Firm", "male"),
        Voice("Aoede", "Breezy", "female"),
        Voice("Autonoe", "Bright", "female"),
        Voice("Callirrhoe", "Easy-going", "female"),
        Voice("Charon", "Informative", "male"),
        Voice("Despina", "Smooth", "female"),
        Voice("Enceladus", "Breathy", "male"),
        Voice("Erinome", "Clear", "female"),
        Voice("Fenrir", "Excitable", "male"),
        Voice("Gacrux", "Mature", "female"),
        Voice("Iapetus", "Clear", "male"),
        Voice("Kore", "Firm", "female"),
        Voice("Laomedeia", "Upbeat", "female"),
        Voice("Leda", "Youthful", "female"),
        Voice("Orus", "Firm", "male"),
        Voice("Pulcherrima", "Forward", "female"),
        Voice("Puck", "Upbeat", "male"),
        Voice("Rasalgethi", "Informative", "male"),
        Voice("Sadachbia", "Lively", "male"),
        Voice("Sadaltager", "Knowledgeable", "male"),
        Voice("Schedar", "Even", "male"),
        Voice("Sulafat", "Warm", "female"),
        Voice("Umbriel", "Easy-going", "male"),
        Voice("Vindemiatrix", "Gentle", "female"),
        Voice("Zephyr", "Bright", "female"),
        Voice("Zubenelgenubi", "Casual", "male"),
    ]
}


@dataclass(frozen=True)
class Tool:
    """A function the assistant may call, described with a JSON schema for its arguments."""

    name: str
    description: str
    parameters: dict[str, Any] = field(default_factory=lambda: {"type": "object", "properties": {}})
