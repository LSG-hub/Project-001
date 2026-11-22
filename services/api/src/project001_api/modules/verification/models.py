from dataclasses import asdict, dataclass, replace
from datetime import datetime
from enum import StrEnum
from typing import Any


class Method(StrEnum):
    # We call the number and read out a 6-digit code; works on any phone.
    VOICE_CODE = "voice_code"
    # The phone calls our number; the carrier's caller ID proves the SIM. Free both ways.
    MISSED_CALL = "missed_call"


class Status(StrEnum):
    PENDING = "pending"
    VERIFIED = "verified"
    FAILED = "failed"
    EXPIRED = "expired"


@dataclass(frozen=True)
class Verification:
    id: str
    phone: str
    method: Method
    status: Status
    created_at: datetime
    expires_at: datetime
    attempts: int = 0
    code_hash: str | None = None
    uid: str | None = None
    # The sign-in token is handed out once, then this flips to True.
    token_issued: bool = False

    def with_(self, **changes: Any) -> "Verification":
        return replace(self, **changes)

    def to_doc(self) -> dict[str, Any]:
        doc = asdict(self)
        doc["method"] = self.method.value
        doc["status"] = self.status.value
        return doc

    @classmethod
    def from_doc(cls, doc: dict[str, Any]) -> "Verification":
        return cls(**{**doc, "method": Method(doc["method"]), "status": Status(doc["status"])})
