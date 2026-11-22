"""Where accounts live. One account per phone number, enforced by keying an index on
the E.164 number: `phone_index/{e164} -> uid`, `accounts/{uid} -> {phone, created_at}`."""

from dataclasses import dataclass
from datetime import datetime
from typing import Protocol

from google.cloud import firestore


@dataclass(frozen=True)
class Account:
    uid: str
    phone: str
    created_at: datetime


class AccountRepository(Protocol):
    async def get_or_create(self, phone: str, new_uid: str, now: datetime) -> Account:
        """The account for `phone`; creates it with `new_uid` if there is none."""
        ...

    async def get(self, uid: str) -> Account | None: ...


class InMemoryAccountRepository:
    def __init__(self) -> None:
        self._by_phone: dict[str, Account] = {}

    async def get_or_create(self, phone: str, new_uid: str, now: datetime) -> Account:
        return self._by_phone.setdefault(phone, Account(new_uid, phone, now))

    async def get(self, uid: str) -> Account | None:
        return next((a for a in self._by_phone.values() if a.uid == uid), None)


class FirestoreAccountRepository:
    def __init__(self, db: firestore.AsyncClient) -> None:
        self._db = db

    async def get_or_create(self, phone: str, new_uid: str, now: datetime) -> Account:
        index_ref = self._db.collection("phone_index").document(phone)
        db = self._db

        @firestore.async_transactional
        async def create_once(transaction: firestore.AsyncTransaction) -> Account:
            snap = await index_ref.get(transaction=transaction)
            if snap.exists:
                uid = str(snap.get("uid"))
                account = await db.collection("accounts").document(uid).get(transaction=transaction)
                return Account(uid, phone, account.get("created_at"))
            transaction.create(index_ref, {"uid": new_uid, "created_at": now})
            transaction.create(db.collection("accounts").document(new_uid), {"phone": phone, "created_at": now})
            return Account(new_uid, phone, now)

        result: Account = await create_once(db.transaction())
        return result

    async def get(self, uid: str) -> Account | None:
        snap = await self._db.collection("accounts").document(uid).get()
        if not snap.exists:
            return None
        return Account(uid, str(snap.get("phone")), snap.get("created_at"))
