"""The phone line. Only a stand-in exists until the virtual number is live.

The real adapter will place calls through the telephony provider (and receive its
webhooks for inbound calls). It must offer the same methods as FakeTelephony.
"""

import logging

from project001_api.core.phone import mask

log = logging.getLogger(__name__)


class FakeTelephony:
    """Places no calls. Locally and on dev it pretends to, and logs the voice code
    so you can sign in; on prod it reports that calls aren't available."""

    def __init__(self, *, pretend: bool) -> None:
        self.can_place_calls = pretend

    async def speak_code(self, to: str, code: str, language: str | None) -> None:
        log.info(
            "Fake call: would read a code aloud",
            extra={"to": mask(to), "code": code, "language": language},
        )
