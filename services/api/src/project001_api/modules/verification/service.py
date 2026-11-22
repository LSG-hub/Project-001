"""Proving someone owns a phone number, then signing them in.

Two ways, both proven by the phone network rather than by the app:
- Voice code: we call the number and read a 6-digit code; they type it in.
- Missed call: their phone calls our number; the carrier's caller ID proves the SIM.
  The telephony webhook reports the caller and `confirm_inbound_call` matches it.
"""

import hashlib
import hmac
import logging
import secrets
from dataclasses import dataclass
from datetime import timedelta

from project001_api.core.clock import Clock
from project001_api.core.errors import AppError, NotFound, TooManyRequests, Unavailable
from project001_api.core.phone import InvalidPhone, mask, to_e164
from project001_api.modules.verification.models import Method, Status, Verification
from project001_api.modules.verification.ports import AccountDirectory, CodeCaller, TokenIssuer
from project001_api.modules.verification.repository import VerificationRepository

log = logging.getLogger(__name__)


@dataclass(frozen=True)
class Limits:
    ttl: timedelta = timedelta(minutes=10)
    max_attempts: int = 5
    max_starts_per_hour: int = 5


@dataclass(frozen=True)
class Outcome:
    verification: Verification
    token: str | None = None


class VerificationService:
    def __init__(
        self,
        repo: VerificationRepository,
        caller: CodeCaller,
        accounts: AccountDirectory,
        tokens: TokenIssuer,
        clock: Clock,
        *,
        inbound_number: str,
        limits: Limits = Limits(),
        accept_any_code: bool = False,
    ) -> None:
        self._repo = repo
        self._caller = caller
        self._accounts = accounts
        self._tokens = tokens
        self._clock = clock
        self._inbound_number = inbound_number
        self._limits = limits
        # Dev only (the container never sets it on prod): any 6-digit code is accepted.
        self._accept_any_code = accept_any_code

    @property
    def inbound_number(self) -> str:
        return self._inbound_number

    async def start(self, phone: str, region: str | None, method: Method, language: str | None) -> Verification:
        e164 = to_e164(phone, region)
        now = self._clock.now()

        recent = [v for v in await self._repo.for_phone(e164) if v.created_at > now - timedelta(hours=1)]
        if len(recent) >= self._limits.max_starts_per_hour:
            raise TooManyRequests("too_many_verifications", "Too many tries for this number. Wait an hour.")

        if method is Method.VOICE_CODE and not self._caller.can_place_calls:
            raise Unavailable("calls_unavailable", "Voice codes aren't available yet.")

        verification = Verification(
            id=secrets.token_urlsafe(18),
            phone=e164,
            method=method,
            status=Status.PENDING,
            created_at=now,
            expires_at=now + self._limits.ttl,
        )
        if method is Method.VOICE_CODE:
            code = f"{secrets.randbelow(1_000_000):06d}"
            verification = verification.with_(code_hash=_hash(verification.id, code))
            await self._repo.save(verification)
            await self._caller.speak_code(e164, code, language)
        else:
            await self._repo.save(verification)
        log.info("Verification started", extra={"phone": mask(e164), "method": method.value})
        return verification

    async def check_code(self, verification_id: str, code: str) -> Outcome:
        verification = await self._live(verification_id)
        if verification.method is not Method.VOICE_CODE or verification.code_hash is None:
            raise AppError("wrong_method", "This verification doesn't use a code.")

        if self._accept_any_code or hmac.compare_digest(verification.code_hash, _hash(verification.id, code)):
            return await self._verify(verification)

        attempts = verification.attempts + 1
        if attempts >= self._limits.max_attempts:
            await self._repo.save(verification.with_(attempts=attempts, status=Status.FAILED))
            raise AppError("verification_failed", "Too many wrong codes. Start again.")
        await self._repo.save(verification.with_(attempts=attempts))
        raise AppError("wrong_code", "That code didn't match.")

    async def confirm_inbound_call(self, caller: str) -> Verification | None:
        """Called by the telephony webhook when a phone calls our number."""
        try:
            e164 = to_e164(caller, mobile_only=False)
        except InvalidPhone:
            return None
        now = self._clock.now()
        pending = [
            v
            for v in await self._repo.for_phone(e164)
            if v.method is Method.MISSED_CALL and v.status is Status.PENDING and v.expires_at > now
        ]
        if not pending:
            return None
        newest = pending[0]
        uid = await self._accounts.account_for_phone(e164)
        verified = newest.with_(status=Status.VERIFIED, uid=uid)
        await self._repo.save(verified)
        log.info("Verified by missed call", extra={"phone": mask(e164)})
        return verified

    async def status(self, verification_id: str) -> Outcome:
        """For the app to poll during a missed-call verification. Hands out the token once."""
        verification = await self._get(verification_id)
        if verification.status is Status.PENDING and verification.expires_at <= self._clock.now():
            verification = verification.with_(status=Status.EXPIRED)
            await self._repo.save(verification)
        if verification.status is Status.VERIFIED and not verification.token_issued and verification.uid:
            return await self._hand_out_token(verification, verification.uid)
        return Outcome(verification)

    async def _verify(self, verification: Verification) -> Outcome:
        uid = await self._accounts.account_for_phone(verification.phone)
        log.info("Verified by voice code", extra={"phone": mask(verification.phone)})
        return await self._hand_out_token(verification.with_(status=Status.VERIFIED, uid=uid), uid)

    async def _hand_out_token(self, verification: Verification, uid: str) -> Outcome:
        token = await self._tokens.issue(uid)
        verification = verification.with_(token_issued=True)
        await self._repo.save(verification)
        return Outcome(verification, token)

    async def _get(self, verification_id: str) -> Verification:
        verification = await self._repo.get(verification_id)
        if verification is None:
            raise NotFound("verification_not_found", "Start again.")
        return verification

    async def _live(self, verification_id: str) -> Verification:
        """A verification that can still be completed."""
        verification = await self._get(verification_id)
        if verification.status is not Status.PENDING:
            raise AppError("verification_closed", "This verification is finished. Start again.")
        if verification.expires_at <= self._clock.now():
            await self._repo.save(verification.with_(status=Status.EXPIRED))
            raise AppError("verification_expired", "That took too long. Start again.", status=410)
        return verification


def _hash(verification_id: str, code: str) -> str:
    return hashlib.sha256(f"{verification_id}:{code}".encode()).hexdigest()
