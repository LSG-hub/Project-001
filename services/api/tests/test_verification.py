import pytest

from project001_api.container import Container, build_container
from project001_api.core.clock import FakeClock
from project001_api.core.errors import AppError, TooManyRequests, Unavailable
from project001_api.core.settings import Env, Settings
from project001_api.modules.verification.models import Method, Status
from tests.conftest import RecordingTelephony

PHONE = "+919876543210"


async def test_voice_code_signs_in_and_reuses_the_account(container: Container, telephony: RecordingTelephony) -> None:
    service = container.verification
    first = await service.start("98765 43210", "IN", Method.VOICE_CODE, "hi-IN")
    assert first.phone == PHONE
    outcome = await service.check_code(first.id, telephony.codes[PHONE])
    assert outcome.verification.status is Status.VERIFIED
    assert outcome.token and outcome.token.startswith("fake.u_")

    second = await service.start(PHONE, None, Method.VOICE_CODE, None)
    again = await service.check_code(second.id, telephony.codes[PHONE])
    assert again.token == outcome.token  # same number, same account


async def test_code_is_stored_only_as_a_hash(container: Container, telephony: RecordingTelephony) -> None:
    v = await container.verification.start(PHONE, None, Method.VOICE_CODE, None)
    assert v.code_hash and telephony.codes[PHONE] not in v.code_hash


async def test_wrong_codes_then_locked(container: Container, telephony: RecordingTelephony) -> None:
    service = container.verification
    v = await service.start(PHONE, None, Method.VOICE_CODE, None)
    wrong = "000000" if telephony.codes[PHONE] != "000000" else "111111"
    for _ in range(4):
        with pytest.raises(AppError) as e:
            await service.check_code(v.id, wrong)
        assert e.value.code == "wrong_code"
    with pytest.raises(AppError) as e:
        await service.check_code(v.id, wrong)
    assert e.value.code == "verification_failed"
    with pytest.raises(AppError) as e:  # even the right code is refused now
        await service.check_code(v.id, telephony.codes[PHONE])
    assert e.value.code == "verification_closed"


async def test_code_expires(container: Container, telephony: RecordingTelephony, clock: FakeClock) -> None:
    v = await container.verification.start(PHONE, None, Method.VOICE_CODE, None)
    clock.advance(minutes=11)
    with pytest.raises(AppError) as e:
        await container.verification.check_code(v.id, telephony.codes[PHONE])
    assert e.value.code == "verification_expired"


async def test_rate_limited_per_number(container: Container, clock: FakeClock) -> None:
    for _ in range(5):
        await container.verification.start(PHONE, None, Method.MISSED_CALL, None)
    with pytest.raises(TooManyRequests):
        await container.verification.start(PHONE, None, Method.MISSED_CALL, None)
    clock.advance(hours=1, seconds=1)
    await container.verification.start(PHONE, None, Method.MISSED_CALL, None)


async def test_missed_call_verifies_and_hands_out_token_once(container: Container) -> None:
    service = container.verification
    v = await service.start(PHONE, None, Method.MISSED_CALL, None)
    assert (await service.status(v.id)).verification.status is Status.PENDING

    assert await service.confirm_inbound_call("+91 98765 43210") is not None
    first = await service.status(v.id)
    assert first.verification.status is Status.VERIFIED and first.token
    assert (await service.status(v.id)).token is None  # not twice


async def test_missed_call_from_another_number_does_nothing(container: Container) -> None:
    service = container.verification
    v = await service.start(PHONE, None, Method.MISSED_CALL, None)
    assert await service.confirm_inbound_call("+919000000000") is None
    assert await service.confirm_inbound_call("withheld") is None
    assert (await service.status(v.id)).verification.status is Status.PENDING


async def test_missed_call_after_expiry_does_nothing(container: Container, clock: FakeClock) -> None:
    v = await container.verification.start(PHONE, None, Method.MISSED_CALL, None)
    clock.advance(minutes=11)
    assert await container.verification.confirm_inbound_call(PHONE) is None
    assert (await container.verification.status(v.id)).verification.status is Status.EXPIRED


async def test_prod_refuses_voice_codes_until_the_phone_line_exists(clock: FakeClock) -> None:
    prod = build_container(Settings(env=Env.PROD, store="memory", auth="fake"), clock=clock)
    with pytest.raises(Unavailable):
        await prod.verification.start(PHONE, None, Method.VOICE_CODE, None)
