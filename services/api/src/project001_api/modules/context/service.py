"""The owner's standing context: who they are, and which calls they're expecting.

The calls module reads this at the start of every call (see `owner_info`) and marks an
expected call as met when a call's summary says it was that call.
"""

import secrets
from dataclasses import dataclass
from datetime import timedelta
from zoneinfo import ZoneInfo, ZoneInfoNotFoundError

from project001_api.core.clock import Clock
from project001_api.core.errors import AppError, NotFound
from project001_api.core.voice import VOICES
from project001_api.modules.context.models import ExpectedCall, ExpectedCallIn, Profile, ProfileIn
from project001_api.modules.context.repository import ContextRepository


@dataclass(frozen=True)
class OwnerInfo:
    name: str | None
    about: str
    timezone: str
    voice: str | None
    expected: tuple[ExpectedCall, ...]


class ContextService:
    def __init__(self, repo: ContextRepository, clock: Clock) -> None:
        self._repo = repo
        self._clock = clock

    async def profile(self, owner_id: str) -> Profile:
        return await self._repo.get_profile(owner_id) or Profile(owner_id=owner_id)

    async def save_profile(self, owner_id: str, update: ProfileIn) -> Profile:
        current = await self.profile(owner_id)
        sent = update.model_fields_set
        timezone = update.timezone if "timezone" in sent and update.timezone else current.timezone
        try:
            ZoneInfo(timezone)
        except (ZoneInfoNotFoundError, ValueError) as e:
            raise AppError("unknown_timezone", f"Unknown time zone {timezone}.", status=422) from e
        voice = (update.voice or None) if "voice" in sent else current.voice
        if voice and voice not in VOICES:
            raise AppError("unknown_voice", f"No voice called {voice}.", status=422)
        profile = Profile(
            owner_id=owner_id,
            name=((update.name or "").strip() or None) if "name" in sent else current.name,
            about=(update.about or "").strip() if "about" in sent else current.about,
            timezone=timezone,
            voice=voice,
            updated_at=self._clock.now(),
        )
        await self._repo.save_profile(profile)
        return profile

    async def expected(self, owner_id: str) -> list[ExpectedCall]:
        """Notes still waiting for their call."""
        now = self._clock.now()
        return [e for e in await self._repo.expected_for(owner_id) if e.is_active(now)]

    async def add_expected(self, owner_id: str, note: ExpectedCallIn) -> ExpectedCall:
        now = self._clock.now()
        expected = ExpectedCall(
            id="e_" + secrets.token_hex(3),
            owner_id=owner_id,
            text=note.text.strip(),
            created_at=now,
            expires_at=now + timedelta(hours=note.hours),
        )
        await self._repo.save_expected(expected)
        return expected

    async def remove_expected(self, owner_id: str, expected_id: str) -> None:
        if not await self._repo.delete_expected(owner_id, expected_id):
            raise NotFound("expected_call_not_found", "No such note.")

    async def owner_info(self, owner_id: str) -> OwnerInfo:
        profile = await self.profile(owner_id)
        return OwnerInfo(
            profile.name, profile.about, profile.timezone, profile.voice, tuple(await self.expected(owner_id))
        )

    async def expectation_met(self, owner_id: str, expected_id: str, call_id: str) -> None:
        for e in await self._repo.expected_for(owner_id):
            if e.id == expected_id and e.met_by_call is None:
                await self._repo.save_expected(
                    e.model_copy(update={"met_by_call": call_id, "met_at": self._clock.now()})
                )
