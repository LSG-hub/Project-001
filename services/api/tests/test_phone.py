import pytest

from project001_api.core.phone import InvalidPhone, mask, to_e164


@pytest.mark.parametrize(
    ("raw", "region", "expected"),
    [
        ("+91 98765 43210", None, "+919876543210"),
        ("98765 43210", "IN", "+919876543210"),
        ("098765-43210", "in", "+919876543210"),
        ("+44 7911 123456", None, "+447911123456"),
        ("(201) 555-0123", "US", "+12015550123"),
    ],
)
def test_normalizes_to_e164(raw: str, region: str | None, expected: str) -> None:
    assert to_e164(raw, region) == expected


@pytest.mark.parametrize(("raw", "region"), [("12345", "IN"), ("not a number", None), ("9876543210", None)])
def test_rejects_invalid(raw: str, region: str | None) -> None:
    with pytest.raises(InvalidPhone):
        to_e164(raw, region)


def test_rejects_landlines_unless_asked() -> None:
    landline = "+91 80 2222 3333"  # Bengaluru
    with pytest.raises(InvalidPhone):
        to_e164(landline)
    assert to_e164(landline, mobile_only=False) == "+918022223333"


def test_mask_hides_the_middle() -> None:
    assert mask("+919876543210") == "+91••••••••10"
