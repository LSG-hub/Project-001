from datetime import datetime
from enum import StrEnum

from pydantic import BaseModel, Field


class Speaker(StrEnum):
    CALLER = "caller"
    ASSISTANT = "assistant"


class Turn(BaseModel):
    speaker: Speaker
    text: str
    # Seconds since the call started.
    at: float
    # The caller talked over the assistant before it finished.
    interrupted: bool = False


class Category(StrEnum):
    SCAM = "scam"
    SPAM = "spam"
    SALES = "sales"
    DELIVERY = "delivery"
    PERSONAL = "personal"
    WORK = "work"
    SERVICE = "service"
    OTHER = "other"


class ScamRisk(StrEnum):
    NONE = "none"
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class CallSummary(BaseModel):
    """What the owner sees after a call. Gemini fills this in from the transcript."""

    summary: str = Field(description="One to three sentences for the phone's owner: who called, why, what happened.")
    caller: str | None = Field(description="The caller's name or organisation, if they gave one.")
    purpose: str = Field(description="Why they called, in a few words.")
    category: Category
    scam_risk: ScamRisk
    scam_signals: list[str] = Field(description="Specific warning signs heard, e.g. 'asked for an OTP'. Empty if none.")
    action_items: list[str] = Field(description="What the owner should do next, if anything. Empty if nothing.")
    language: str = Field(description="Main language the caller spoke, e.g. 'English', 'Hindi'.")
    fulfilled_expected_calls: list[str] = Field(
        default_factory=list,
        description="Ids (like e_1a2b3c) of the owner's expected calls that this call was. Usually empty.",
    )


class Call(BaseModel):
    id: str
    # "test" for calls from the test page or simulator; "phone" once the line is live.
    source: str
    # The account the call belongs to, and who called (E.164, None if withheld).
    owner_id: str | None = None
    caller_number: str | None = None
    owner_name: str | None
    # Gemini Live voice the assistant used; None means Gemini's default.
    voice: str | None = None
    started_at: datetime
    ended_at: datetime
    duration_seconds: float
    # "caller" hung up, "assistant" ended it (end_call tool), "connection" dropped, or "error".
    ended_by: str
    end_reason: str | None = None
    turns: list[Turn]
    summary: CallSummary | None = None
