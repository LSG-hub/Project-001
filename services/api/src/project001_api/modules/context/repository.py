"""Owner context in Firestore: `profiles/{owner_id}` and `expected_calls/{id}`.

Expected calls are looked up with one equality filter (owner_id), so Firestore needs no
composite index. Old ones can be removed with a TTL policy on `expires_at`.
"""

from typing import Protocol

from google.cloud import firestore

from project001_api.modules.context.models import ExpectedCall, Profile


class ContextRepository(Protocol):
    async def get_profile(self, owner_id: str) -> Profile | None: ...

    async def save_profile(self, profile: Profile) -> None: ...

    async def save_expected(self, expected: ExpectedCall) -> None: ...

    async def expected_for(self, owner_id: str) -> list[ExpectedCall]: ...

    async def delete_expected(self, owner_id: str, expected_id: str) -> bool: ...


class InMemoryContextRepository:
    def __init__(self) -> None:
        self._profiles: dict[str, Profile] = {}
        self._expected: dict[str, ExpectedCall] = {}

    async def get_profile(self, owner_id: str) -> Profile | None:
        return self._profiles.get(owner_id)

    async def save_profile(self, profile: Profile) -> None:
        self._profiles[profile.owner_id] = profile

    async def save_expected(self, expected: ExpectedCall) -> None:
        self._expected[expected.id] = expected

    async def expected_for(self, owner_id: str) -> list[ExpectedCall]:
        return sorted((e for e in self._expected.values() if e.owner_id == owner_id), key=lambda e: e.created_at)

    async def delete_expected(self, owner_id: str, expected_id: str) -> bool:
        found = self._expected.get(expected_id)
        if found is None or found.owner_id != owner_id:
            return False
        del self._expected[expected_id]
        return True


class FirestoreContextRepository:
    def __init__(self, db: firestore.AsyncClient) -> None:
        self._profiles = db.collection("profiles")
        self._expected = db.collection("expected_calls")

    async def get_profile(self, owner_id: str) -> Profile | None:
        snap = await self._profiles.document(owner_id).get()
        return Profile.model_validate(snap.to_dict()) if snap.exists else None

    async def save_profile(self, profile: Profile) -> None:
        await self._profiles.document(profile.owner_id).set(profile.model_dump(mode="json"))

    async def save_expected(self, expected: ExpectedCall) -> None:
        await self._expected.document(expected.id).set(expected.model_dump(mode="json"))

    async def expected_for(self, owner_id: str) -> list[ExpectedCall]:
        query = self._expected.where(filter=firestore.FieldFilter("owner_id", "==", owner_id)).limit(200)
        found = [ExpectedCall.model_validate(s.to_dict()) async for s in query.stream()]
        return sorted(found, key=lambda e: e.created_at)

    async def delete_expected(self, owner_id: str, expected_id: str) -> bool:
        ref = self._expected.document(expected_id)
        snap = await ref.get()
        if not snap.exists or (snap.to_dict() or {}).get("owner_id") != owner_id:
            return False
        await ref.delete()
        return True
