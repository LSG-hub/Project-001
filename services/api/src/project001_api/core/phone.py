"""Phone numbers. Every number is stored and compared in E.164 ("+919876543210"),
so one number written two ways can never become two accounts."""

import phonenumbers
from phonenumbers import NumberParseException, PhoneNumberType

from project001_api.core.errors import AppError

_MOBILE_TYPES = {PhoneNumberType.MOBILE, PhoneNumberType.FIXED_LINE_OR_MOBILE}


class InvalidPhone(AppError):
    def __init__(self, message: str = "That doesn't look like a mobile number.") -> None:
        super().__init__("invalid_phone", message, status=422)


def to_e164(raw: str, region: str | None = None, *, mobile_only: bool = True) -> str:
    """Parse `raw` ("+91 98765 43210", or "98765 43210" with region "IN") into E.164.

    Raises InvalidPhone for anything that isn't a real number, and for landlines when
    `mobile_only` (some countries, like the US, can't tell the two apart; those pass).
    """
    try:
        number = phonenumbers.parse(raw, region.upper() if region else None)
    except NumberParseException as e:
        raise InvalidPhone() from e
    if not phonenumbers.is_valid_number(number):
        raise InvalidPhone()
    if mobile_only and phonenumbers.number_type(number) not in _MOBILE_TYPES:
        raise InvalidPhone()
    return phonenumbers.format_number(number, phonenumbers.PhoneNumberFormat.E164)


def mask(e164: str) -> str:
    """For logs: keep the country code and last two digits only."""
    return e164[:3] + "•" * max(len(e164) - 5, 0) + e164[-2:]
