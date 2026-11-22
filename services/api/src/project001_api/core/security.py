"""Who is calling the API, from the Bearer token."""

from collections.abc import Awaitable, Callable
from typing import Annotated

from fastapi import Header

from project001_api.core.errors import Unauthorized

TokenVerifier = Callable[[str], Awaitable[str]]
CurrentUid = Callable[..., Awaitable[str]]


def make_current_uid(verify: TokenVerifier) -> CurrentUid:
    async def current_uid(authorization: Annotated[str | None, Header()] = None) -> str:
        if not authorization or not authorization.startswith("Bearer "):
            raise Unauthorized("missing_token", "Sign in first.")
        return await verify(authorization.removeprefix("Bearer ").strip())

    return current_uid


def make_current_owner(verify: TokenVerifier, *, dev_owner: str | None) -> CurrentUid:
    """The account whose data a request reads or changes.

    A signed-in user is always their own owner. Without a token, local dev tools act as
    `dev_owner` (so the test page works without signing in); everywhere else it's 401.
    """

    async def current_owner(authorization: Annotated[str | None, Header()] = None) -> str:
        if authorization and authorization.startswith("Bearer "):
            return await verify(authorization.removeprefix("Bearer ").strip())
        if dev_owner:
            return dev_owner
        raise Unauthorized("missing_token", "Sign in first.")

    return current_owner
