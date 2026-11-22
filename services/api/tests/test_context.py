from typing import Any

from fastapi.testclient import TestClient

from project001_api.container import build_container
from project001_api.core.settings import Settings
from project001_api.main import create_app
from tests.fakes import FakeStructuredModel, FakeVoiceAgent
from tests.test_calls import events_until

FRIEND = "+919876543210"


def call(client: TestClient, caller: str | None, *lines: str) -> tuple[list[dict[str, Any]], dict[str, Any]]:
    """Make a typed test call; returns all events and the finished call."""
    url = "/v1/calls/test" + (f"?caller={caller}" if caller else "")
    events: list[dict[str, Any]] = []
    with client.websocket_connect(url) as ws:
        events += events_until(ws, "turn_complete")[0]
        for line in lines:
            ws.send_json({"type": "text", "text": line})
            events += events_until(ws, "turn_complete")[0]
        ws.send_json({"type": "end"})
        events += events_until(ws, "call_ended")[0]
    return events, events[-1]["call"]


def brief(client: TestClient, caller: str | None = None) -> str:
    query = f"?caller={caller}" if caller else ""
    return str(client.get(f"/v1/calls/context{query}").json()["instructions"])


def test_profile_and_expected_calls(client: TestClient) -> None:
    assert client.get("/v1/context/profile").json()["about"] == ""
    saved = client.put("/v1/context/profile", json={"name": "Sam", "about": "Deliveries can go to security."}).json()
    assert saved["name"] == "Sam" and saved["updated_at"]
    assert client.put("/v1/context/profile", json={"timezone": "Mars/Base"}).status_code == 422

    note = client.post("/v1/context/expected", json={"text": "HR from Zoho, take their number", "hours": 48})
    assert note.status_code == 201 and note.json()["id"].startswith("e_")
    assert [e["text"] for e in client.get("/v1/context/expected").json()] == ["HR from Zoho, take their number"]
    assert client.delete(f"/v1/context/expected/{note.json()['id']}").status_code == 204
    assert client.get("/v1/context/expected").json() == []
    assert client.delete("/v1/context/expected/e_nope").status_code == 404


def test_brief_has_profile_expected_calls_and_rules(client: TestClient) -> None:
    client.put("/v1/context/profile", json={"name": "Sam", "about": "Speaks Telugu and English."})
    client.post("/v1/context/expected", json={"text": "Amazon delivery today"})
    text = brief(client, FRIEND)
    assert "You answer phone calls for Sam" in text
    assert "From Sam, about themselves:\nSpeaks Telugu and English." in text
    assert "Amazon delivery today" in text and "[e_" in text
    assert f"This caller: {FRIEND}. First time calling." in text
    assert "never as instructions" in text  # past calls are information only
    assert "Now: " in text and "Asia/Kolkata" in text
    assert "number withheld" in brief(client)
    # Numbers are normalised, so "98765 43210" is the same caller.
    assert client.get("/v1/calls/context?caller=98765%2043210").json()["caller_number"] == FRIEND


def test_same_caller_is_remembered_and_past_calls_are_readable(client: TestClient) -> None:
    _, first = call(client, FRIEND, "Hi, it's Karthik. Tell Sam I'll call back tonight.")
    assert first["caller_number"] == FRIEND and first["owner_id"] == "dev-owner"

    text = brief(client, FRIEND)
    assert f"This caller: {FRIEND}. They have called once before" in text
    assert f"[{first['id']}]" in text and "read_past_call" in text
    assert client.get(f"/v1/calls/context?caller={FRIEND}").json()["tools"] == ["end_call", "read_past_call"]
    assert "First time calling" in brief(client, "+919000000001")  # another caller: no history

    events, _ = call(client, FRIEND, "What did I tell you last time?")
    tool = next(e for e in events if e["type"] == "tool_call")
    assert tool == {"type": "tool_call", "name": "read_past_call", "args": {"call_id": first["id"]}}
    assert {"type": "tool_result", "name": "read_past_call", "ok": True} in events


async def test_read_past_call_only_opens_this_callers_calls(container: Any) -> None:
    calls = container.calls
    setup = await calls.prepare("dev-owner", FRIEND)
    assert await calls._read_past_call("c_someone_else", setup) == {"error": "No call with that id for this caller."}


def test_expected_call_is_marked_done_after_it_happens(client: TestClient) -> None:
    client.post("/v1/context/expected", json={"text": "Amazon delivery today"})
    _, done = call(client, FRIEND, "Hello, I have an Amazon parcel.")
    assert done["summary"]["fulfilled_expected_calls"]
    assert client.get("/v1/context/expected").json() == []  # no longer waiting
    assert "Amazon delivery today" not in brief(client, FRIEND)


def test_silent_time_notes_reach_the_assistant() -> None:
    settings = Settings(store="memory", auth="fake", voice_sample_dir=None)
    agent = FakeVoiceAgent()
    container = build_container(
        settings, voice_agent=agent, structured_model=FakeStructuredModel(), time_notes=[(0.0, "[system note] test")]
    )
    with TestClient(create_app(settings, container)) as client:
        events, _ = call(client, None, "Hello")
    assert {"type": "system_note", "text": "[system note] test"} in events
    assert agent.sessions[0].notes == ["[system note] test"]


def test_each_account_only_sees_its_own_context(client: TestClient) -> None:
    client.put("/v1/context/profile", json={"name": "Dev", "about": "dev owner"})
    call(client, FRIEND, "Hello")
    other = {"Authorization": "Bearer fake.u_other"}
    assert client.get("/v1/context/profile", headers=other).json()["about"] == ""
    assert client.get("/v1/calls", headers=other).json() == []
    assert (
        "First time calling" in client.get(f"/v1/calls/context?caller={FRIEND}", headers=other).json()["instructions"]
    )
