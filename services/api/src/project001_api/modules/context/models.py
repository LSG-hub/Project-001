from datetime import datetime

from pydantic import BaseModel, Field


class Profile(BaseModel):
    """What the owner tells the assistant about themselves. Their words are instructions."""

    owner_id: str
    name: str | None = None
    # Free text: who they are, languages, preferences, what may or may not be shared.
    about: str = ""
    timezone: str = "Asia/Kolkata"
    # The assistant's voice on your calls (a Gemini voice name); None means the default.
    voice: str | None = None
    updated_at: datetime | None = None


class ExpectedCall(BaseModel):
    """A call the owner is expecting, e.g. "Delivery from Amazon today, leave it with security"."""

    id: str
    owner_id: str
    text: str
    created_at: datetime
    expires_at: datetime
    # Set when a call matched this note (from the call's summary).
    met_by_call: str | None = None
    met_at: datetime | None = None

    def is_active(self, now: datetime) -> bool:
        return self.met_by_call is None and self.expires_at > now


class ProfileIn(BaseModel):
    """A partial update: only the fields you send change. Send "" or null to clear name or voice."""

    name: str | None = Field(default=None, max_length=60)
    about: str | None = Field(default=None, max_length=2000)
    timezone: str | None = Field(default=None, max_length=40)
    voice: str | None = Field(default=None, max_length=40)


class ExpectedCallIn(BaseModel):
    text: str = Field(min_length=3, max_length=500)
    # How long the note stays active, in hours (default: a day).
    hours: float = Field(default=24, gt=0, le=24 * 30)
