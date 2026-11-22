"""Finished calls, in the `calls` collection (one document per call, transcript included).

Lookups use one equality filter (owner_id or caller_number), so Firestore needs no
composite index; results are sorted here.
"""

from typing import Protocol

from google.cloud import firestore

from project001_api.modules.calls.models import Call

COLLECTION = "calls"
MAX_HISTORY = 20


class CallRepository(Protocol):
    async def save(self, call: Call) -> None: ...

    async def get(self, call_id: str) -> Call | None: ...

    async def recent(self, owner_id: str, limit: int) -> list[Call]: ...

    async def for_caller(self, owner_id: str, caller_number: str) -> list[Call]:
        """Earlier calls from `caller_number` to `owner_id`, newest first (at most 20)."""
        ...


class InMemoryCallRepository:
    def __init__(self) -> None:
        self._calls: dict[str, Call] = {}

    async def save(self, call: Call) -> None:
        self._calls[call.id] = call

    async def get(self, call_id: str) -> Call | None:
        return self._calls.get(call_id)

    async def recent(self, owner_id: str, limit: int) -> list[Call]:
        mine = [c for c in self._calls.values() if c.owner_id == owner_id]
        return sorted(mine, key=lambda c: c.started_at, reverse=True)[:limit]

    async def for_caller(self, owner_id: str, caller_number: str) -> list[Call]:
        found = [c for c in self._calls.values() if c.owner_id == owner_id and c.caller_number == caller_number]
        return sorted(found, key=lambda c: c.started_at, reverse=True)[:MAX_HISTORY]


class FirestoreCallRepository:
    def __init__(self, db: firestore.AsyncClient) -> None:
        self._col = db.collection(COLLECTION)

    async def save(self, call: Call) -> None:
        await self._col.document(call.id).set(call.model_dump(mode="json"))

    async def get(self, call_id: str) -> Call | None:
        snap = await self._col.document(call_id).get()
        return Call.model_validate(snap.to_dict()) if snap.exists else None

    async def recent(self, owner_id: str, limit: int) -> list[Call]:
        # One equality filter needs no composite index; sort here instead.
        query = self._col.where(filter=firestore.FieldFilter("owner_id", "==", owner_id)).limit(200)
        found = [Call.model_validate(s.to_dict()) async for s in query.stream()]
        return sorted(found, key=lambda c: c.started_at, reverse=True)[:limit]

    async def for_caller(self, owner_id: str, caller_number: str) -> list[Call]:
        query = self._col.where(filter=firestore.FieldFilter("caller_number", "==", caller_number)).limit(200)
        found = [Call.model_validate(s.to_dict()) async for s in query.stream()]
        mine = [c for c in found if c.owner_id == owner_id]  # never another owner's calls
        return sorted(mine, key=lambda c: c.started_at, reverse=True)[:MAX_HISTORY]
