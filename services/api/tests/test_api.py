from fastapi.testclient import TestClient

from project001_api.container import Container
from tests.conftest import RecordingTelephony


def test_healthz(client: TestClient) -> None:
    response = client.get("/health")
    assert response.status_code == 200
    assert response.json()["status"] == "ok"


def test_sign_in_with_a_voice_code_then_call_me(client: TestClient, telephony: RecordingTelephony) -> None:
    started = client.post("/v1/verifications", json={"phone": "98765 43210", "region": "IN", "method": "voice_code"})
    assert started.status_code == 201, started.text
    body = started.json()
    assert body["call_to"] is None

    checked = client.post(f"/v1/verifications/{body['id']}/code", json={"code": telephony.codes["+919876543210"]})
    assert checked.status_code == 200
    token = checked.json()["token"]

    me = client.get("/v1/me", headers={"Authorization": f"Bearer {token}"})
    assert me.status_code == 200
    assert me.json()["phone"] == "+919876543210"


def test_missed_call_flow(client: TestClient, container: Container) -> None:
    started = client.post("/v1/verifications", json={"phone": "+919876543210"}).json()
    assert started["method"] == "missed_call"
    assert started["call_to"] == "+919999900001"
    assert client.get(f"/v1/verifications/{started['id']}").json()["status"] == "pending"

    client.portal.call(container.verification.confirm_inbound_call, "+919876543210")  # type: ignore[union-attr]
    done = client.get(f"/v1/verifications/{started['id']}").json()
    assert done["status"] == "verified" and done["token"]


def test_errors_have_one_shape(client: TestClient) -> None:
    bad_phone = client.post("/v1/verifications", json={"phone": "12345", "region": "IN"})
    assert bad_phone.status_code == 422
    assert bad_phone.json()["error"]["code"] == "invalid_phone"

    bad_code = client.post("/v1/verifications/nope/code", json={"code": "12"})
    assert bad_code.status_code == 422
    assert bad_code.json()["error"]["code"] == "invalid_request"

    missing = client.post("/v1/verifications/nope/code", json={"code": "123456"})
    assert missing.status_code == 404
    assert missing.json()["error"]["code"] == "verification_not_found"


def test_me_needs_a_token(client: TestClient) -> None:
    assert client.get("/v1/me").json()["error"]["code"] == "missing_token"
    assert client.get("/v1/me", headers={"Authorization": "Bearer junk"}).status_code == 401
