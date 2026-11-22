"""Sign-in tokens.

After a number is verified, the API mints a Firebase Auth custom token for that
account. The app signs in to Firebase Auth with it (no SMS, so no per-sign-in cost)
and sends the resulting ID token on every request; the API verifies it.
"""

import asyncio
from typing import Any

import firebase_admin
from firebase_admin import auth

from project001_api.core.errors import Unauthorized


class FirebaseTokens:
    def __init__(self, project: str, signer: str | None) -> None:
        options: dict[str, Any] = {"projectId": project}
        if signer:
            # On Cloud Run there is no private key: tokens are signed through IAM
            # (signBlob) as this service account.
            options["serviceAccountId"] = signer
        self._app = firebase_admin.initialize_app(options=options, name="project001-api")

    async def issue(self, uid: str) -> str:
        token: bytes = await asyncio.to_thread(auth.create_custom_token, uid, app=self._app)
        return token.decode()

    async def verify(self, id_token: str) -> str:
        """Returns the uid of a valid Firebase ID token."""
        try:
            claims: dict[str, Any] = await asyncio.to_thread(auth.verify_id_token, id_token, app=self._app)
        except (ValueError, auth.InvalidIdTokenError, auth.ExpiredIdTokenError, auth.RevokedIdTokenError) as e:
            raise Unauthorized("invalid_token", "Sign in again.") from e
        return str(claims["uid"])


class FakeTokens:
    """Local runs and tests: the token is just "fake.<uid>"."""

    async def issue(self, uid: str) -> str:
        return f"fake.{uid}"

    async def verify(self, id_token: str) -> str:
        if not id_token.startswith("fake."):
            raise Unauthorized("invalid_token", "Sign in again.")
        return id_token.removeprefix("fake.")
