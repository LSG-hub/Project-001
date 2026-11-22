"""What the Android app relies on, as configured on Cloud Run dev and prod."""

import pytest
from fastapi.testclient import TestClient
from starlette.websockets import WebSocketDisconnect

from project001_api.container import build_container
from project001_api.core.errors import AppError
from project001_api.core.settings import Env, Settings
from project001_api.main import create_app
from project001_api.modules.verification.models import Method, Status
from tests.conftest import RecordingTelephony
from tests.fakes import FakeStructuredModel, FakeVoiceAgent
from tests.test_calls import events_until

ME = {"Authorization": "Bearer fake.u_me"}


def app_for(env: Env, **overrides: object) -> tuple[TestClient, FakeVoiceAgent]:
    settings = Settings(env=env, store="memory", auth="fake", voice_sample_dir=None, **overrides)  # type: ignore[arg-type]
    agent = FakeVoiceAgent()
    container = build_container(settings, voice_agent=agent, structured_model=FakeStructuredModel())
    return TestClient(create_app(settings, container)), agent


def test_signed_in_test_call_on_dev_uses_my_account_and_voice() -> None:
    client, agent = app_for(Env.DEV, test_calls=True)
    with client:
        assert client.put("/v1/context/profile", headers=ME, json={"name": "Sam", "voice": "Charon"}).status_code == 200
        with client.websocket_connect("/v1/calls/test?caller=9876543210", headers=ME) as ws:
            events_until(ws, "turn_complete")
            ws.send_json({"type": "text", "text": "Hi, parcel for Sam."})
            events_until(ws, "turn_complete")
            ws.send_json({"type": "end"})
            ended, _ = events_until(ws, "call_ended")
        call = ended[-1]["call"]
        assert call["owner_id"] == "u_me" and call["voice"] == "Charon" and call["caller_number"] == "+919876543210"
        assert agent.voices == ["Charon"]
        assert [c["id"] for c in client.get("/v1/calls", headers=ME).json()] == [call["id"]]
        assert (
            client.get(f"/v1/calls/{call['id']}", headers={"Authorization": "Bearer fake.u_other"}).status_code == 404
        )


def test_dev_test_calls_need_sign_in() -> None:
    client, agent = app_for(Env.DEV, test_calls=True)
    with client, client.websocket_connect("/v1/calls/test") as ws:
        assert ws.receive_json() == {"type": "error", "message": "Sign in first."}
    assert agent.voices == []


def test_prod_has_no_test_calls_even_if_asked() -> None:
    client, _ = app_for(Env.PROD, test_calls=True)
    with client, pytest.raises(WebSocketDisconnect), client.websocket_connect("/v1/calls/test", headers=ME) as ws:
        ws.receive_json()


def test_profile_updates_only_what_is_sent() -> None:
    client, _ = app_for(Env.DEV)
    with client:
        client.put("/v1/context/profile", headers=ME, json={"name": "Sam", "about": "Hi", "voice": "Kore"})
        client.put("/v1/context/profile", headers=ME, json={"about": "Updated"})
        profile = client.get("/v1/context/profile", headers=ME).json()
        assert (profile["name"], profile["about"], profile["voice"]) == ("Sam", "Updated", "Kore")
        client.put("/v1/context/profile", headers=ME, json={"voice": None})
        assert client.get("/v1/context/profile", headers=ME).json()["voice"] is None
        bad = client.put("/v1/context/profile", headers=ME, json={"voice": "Nobody"})
        assert bad.json()["error"]["code"] == "unknown_voice"


async def test_any_code_works_on_dev_only() -> None:
    dev = build_container(
        Settings(env=Env.DEV, store="memory", auth="fake", verification_accept_any_code=True),
        telephony=RecordingTelephony(),
    ).verification
    v = await dev.start("+919876543210", None, Method.VOICE_CODE, None)
    assert (await dev.check_code(v.id, "424242")).verification.status is Status.VERIFIED

    prod = build_container(
        Settings(env=Env.PROD, store="memory", auth="fake", verification_accept_any_code=True),
        telephony=RecordingTelephony(),
    ).verification
    v = await prod.start("+919876543210", None, Method.VOICE_CODE, None)
    with pytest.raises(AppError) as e:
        await prod.check_code(v.id, "424242")
    assert e.value.code == "wrong_code"
