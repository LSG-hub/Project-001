"""What verification needs from outside. The container plugs in the real pieces;
this module never imports them, so it stays independent and easy to test."""

from typing import Protocol


class CodeCaller(Protocol):
    """Calls a number and reads a code aloud (the telephony adapter)."""

    can_place_calls: bool

    async def speak_code(self, to: str, code: str, language: str | None) -> None: ...


class TokenIssuer(Protocol):
    """Mints a sign-in token for an account (Firebase custom tokens on Cloud Run)."""

    async def issue(self, uid: str) -> str: ...


class AccountDirectory(Protocol):
    """Finds or creates the account for a verified number (the accounts module)."""

    async def account_for_phone(self, phone: str) -> str: ...
