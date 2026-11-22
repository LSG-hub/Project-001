import secrets

from project001_api.core.clock import Clock
from project001_api.core.errors import NotFound
from project001_api.modules.accounts.repository import Account, AccountRepository


class AccountService:
    def __init__(self, repo: AccountRepository, clock: Clock) -> None:
        self._repo = repo
        self._clock = clock

    async def account_for_phone(self, phone: str) -> str:
        """The uid for a verified E.164 number, creating the account the first time."""
        account = await self._repo.get_or_create(phone, _new_uid(), self._clock.now())
        return account.uid

    async def get(self, uid: str) -> Account:
        account = await self._repo.get(uid)
        if account is None:
            raise NotFound("account_not_found", "No account for this sign-in.")
        return account


def _new_uid() -> str:
    # Random, so the uid (which ends up in tokens) never reveals the phone number.
    return "u_" + secrets.token_hex(12)
