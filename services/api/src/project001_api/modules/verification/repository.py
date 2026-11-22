"""Verification attempts, in the `verifications` collection.

Lookups by phone use a single-field equality filter, so Firestore needs no composite
index. Old documents should be removed with a Firestore TTL policy on `expires_at`.
"""

from typing import Protocol

from google.cloud import firestore

from project001_api.modules.verification.models import Verification

COLLECTION = "verifications"


class VerificationRepository(Protocol):
    async def save(self, verification: Verification) -> None: ...

    async def get(self, verification_id: str) -> Verification | None: ...

    async def for_phone(self, phone: str) -> list[Verification]:
        """Recent attempts for a number, newest first."""
        ...


class InMemoryVerificationRepository:
    def __init__(self) -> None:
        self._items: dict[str, Verification] = {}

    async def save(self, verification: Verification) -> None:
        self._items[verification.id] = verification

    async def get(self, verification_id: str) -> Verification | None:
        return self._items.get(verification_id)

    async def for_phone(self, phone: str) -> list[Verification]:
        found = [v for v in self._items.values() if v.phone == phone]
        return sorted(found, key=lambda v: v.created_at, reverse=True)


class FirestoreVerificationRepository:
    def __init__(self, db: firestore.AsyncClient) -> None:
        self._col = db.collection(COLLECTION)

    async def save(self, verification: Verification) -> None:
        await self._col.document(verification.id).set(verification.to_doc())

    async def get(self, verification_id: str) -> Verification | None:
        snap = await self._col.document(verification_id).get()
        return Verification.from_doc(snap.to_dict() or {}) if snap.exists else None

    async def for_phone(self, phone: str) -> list[Verification]:
        query = self._col.where(filter=firestore.FieldFilter("phone", "==", phone)).limit(50)
        found = [Verification.from_doc(s.to_dict() or {}) async for s in query.stream()]
        return sorted(found, key=lambda v: v.created_at, reverse=True)
