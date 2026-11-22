from pathlib import Path
from typing import Any

from fastapi.testclient import TestClient
from starlette.testclient import WebSocketTestSession

from project001_api.container import build_container
from project001_api.core.settings import Env, Settings
from project001_api.main import create_app
from project001_api.modules.calls.models import Speaker
from project001_api.modules.calls.transcript import TranscriptBuilder
from tests.fakes import FakeStructuredModel, FakeVoiceAgent


def events_until(ws: WebSocketTestSession, stop: str) -> tuple[list[dict[str, Any]], int]:
    """JSON events up to and including `stop`, and how many audio bytes arrived meanwhile."""
    events: list[dict[str, Any]] = []
    audio = 0
    while True:
        message = ws.receive()
        if message.get("bytes") is not None:
            audio += len(message["bytes"])
            continue
        import json

        event = json.loads(message["text"])
        events.append(event)
        if event["type"] == stop:
            return events, audio


def test_typed_call_gets_transcript_and_summary(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    with client.websocket_connect("/v1/calls/test?owner=Sam") as ws:
        started, _ = events_until(ws, "call_started")
        greeting, audio = events_until(ws, "turn_complete")
        assert audio > 0
        assert "".join(e["text"] for e in greeting if e["type"] == "assistant_said").startswith("Hello, this is")

        ws.send_json({"type": "text", "text": "Hi, I have a parcel for Sam."})
        events_until(ws, "turn_complete")
        ws.send_json({"type": "end"})
        ended, _ = events_until(ws, "call_ended")

    call = ended[-1]["call"]
    assert call["id"] == started[0]["id"]
    assert call["ended_by"] == "caller"
    assert [(t["speaker"], t["text"]) for t in call["turns"]] == [
        ("assistant", "Hello, this is Sam's assistant. Who's calling?"),
        ("caller", "Hi, I have a parcel for Sam."),
        ("assistant", "Thanks, I'll pass that on."),
    ]
    assert call["summary"]["category"] == "delivery"
    assert "Sam" in voice_agent.instructions[0]
    assert [t.name for t in voice_agent.tools[0]] == ["end_call"]

    listed = client.get("/v1/calls").json()
    assert listed[0]["id"] == call["id"]
    assert client.get(f"/v1/calls/{call['id']}").json()["turns"] == call["turns"]


def test_assistant_hangs_up_on_a_scammer(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    with client.websocket_connect("/v1/calls/test") as ws:
        events_until(ws, "turn_complete")
        ws.send_json({"type": "text", "text": "Read me the OTP you just got."})
        events, _ = events_until(ws, "call_ended")

    assert any(e["type"] == "tool_call" and e["name"] == "end_call" for e in events)
    call = events[-1]["call"]
    assert call["ended_by"] == "assistant"
    assert call["end_reason"] == "asked for OTP"
    assert call["summary"]["scam_risk"] == "high"
    assert voice_agent.sessions[0].tool_results == [("end_call", {"ok": True})]


def test_spoken_caller_is_transcribed(client: TestClient) -> None:
    with client.websocket_connect("/v1/calls/test") as ws:
        events_until(ws, "turn_complete")
        ws.send_bytes(b"\x01\x00" * 320)
        events, _ = events_until(ws, "turn_complete")
        assert [e["text"] for e in events if e["type"] == "caller_said"] == ["This is", " the courier."]
        ws.send_json({"type": "end"})
        ended, _ = events_until(ws, "call_ended")
    caller_turns = [t["text"] for t in ended[-1]["call"]["turns"] if t["speaker"] == "caller"]
    assert caller_turns == ["This is the courier."]


def test_no_summary_when_the_caller_never_speaks(client: TestClient, structured_model: FakeStructuredModel) -> None:
    with client.websocket_connect("/v1/calls/test") as ws:
        events_until(ws, "turn_complete")
        ws.send_json({"type": "end"})
        ended, _ = events_until(ws, "call_ended")
    assert ended[-1]["call"]["summary"] is None
    assert structured_model.prompts == []


def test_dev_tools_are_off_outside_local() -> None:
    for env in (Env.DEV, Env.PROD):
        settings = Settings(env=env, store="memory", auth="fake")
        app = create_app(settings, build_container(settings, voice_agent=FakeVoiceAgent()))
        with TestClient(app) as c:
            # Real endpoints need sign-in; dev tools don't exist at all.
            assert c.get("/v1/calls").status_code == 401
            assert c.get("/v1/context/profile").status_code == 401
            assert c.get("/dev/call").status_code == 404
            assert c.get("/v1/voices/Kore/sample").status_code == 404


def test_test_page_is_served_locally(client: TestClient) -> None:
    page = client.get("/dev/call")
    assert page.status_code == 200
    assert "/v1/calls/test" in page.text


def test_transcript_builder_joins_fragments_and_marks_interruptions() -> None:
    t = 0.0
    builder = TranscriptBuilder(lambda: t)
    builder.add(Speaker.ASSISTANT, "Hello, this")
    builder.add(Speaker.ASSISTANT, " is Sam's assistant.")
    builder.end_of_turn()
    builder.add(Speaker.ASSISTANT, "How can")
    t = 3.0
    builder.add(Speaker.CALLER, "Sorry to")
    builder.add(Speaker.CALLER, " interrupt.")
    builder.add(Speaker.ASSISTANT, "No problem, I")
    builder.interrupted()
    turns = builder.turns()
    assert [(x.speaker.value, x.text, x.interrupted) for x in turns] == [
        ("assistant", "Hello, this is Sam's assistant.", False),
        ("assistant", "How can", False),
        ("caller", "Sorry to interrupt.", False),
        ("assistant", "No problem, I", True),
    ]
    assert turns[2].at == 3.0


def test_voices_are_listed_and_previewable(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    voices = client.get("/v1/voices").json()
    assert len(voices) == 30
    assert {"name": "Kore", "style": "Firm", "gender": "female", "default": True} in voices
    assert sum(v["default"] for v in voices) == 1
    assert {v["gender"] for v in voices} == {"female", "male"}

    sample = client.get("/v1/voices/Puck/sample?owner=Sam")
    assert sample.status_code == 200
    assert sample.headers["content-type"] == "audio/wav"
    assert sample.content.startswith(b"RIFF")
    client.get("/v1/voices/Puck/sample?owner=Sam")  # cached: no second Live session
    assert voice_agent.voices == ["Puck"]
    script = client.get("/v1/voices/sample-script?owner=Sam").json()["script"]
    assert script.startswith("Hello, this is Sam's assistant") and "Umm" in script
    assert client.get("/v1/voices/Nobody/sample").json()["error"]["code"] == "unknown_voice"


def test_call_uses_the_chosen_voice(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    with client.websocket_connect("/v1/calls/test?voice=Charon") as ws:
        events_until(ws, "turn_complete")
        ws.send_json({"type": "end"})
        ended, _ = events_until(ws, "call_ended")
    assert voice_agent.voices == ["Charon"]
    assert ended[-1]["call"]["voice"] == "Charon"


def test_unknown_voice_is_refused(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    with client.websocket_connect("/v1/calls/test?voice=Nobody") as ws:
        error = ws.receive_json()
    assert error == {"type": "error", "message": "No voice called Nobody."}
    assert voice_agent.voices == []


async def test_voice_samples_are_kept_on_disk(tmp_path: Path) -> None:
    agent = FakeVoiceAgent()
    settings = Settings(env=Env.LOCAL, store="memory", auth="fake", voice_sample_dir=str(tmp_path))
    first = build_container(settings, voice_agent=agent).samples
    sample = await first.voice_sample("Kore", "Sam")
    assert sample.said == "Hello, this is Sam's assistant..."
    assert len(list(tmp_path.iterdir())) == 2  # audio + transcript

    again = await build_container(settings, voice_agent=agent).samples.voice_sample("Kore", "Sam")
    assert again.pcm24k == sample.pcm24k
    assert agent.voices == ["Kore"]  # read from disk, not recorded twice


def test_sample_scripts_come_in_several_languages(client: TestClient) -> None:
    codes = [lang["code"] for lang in client.get("/v1/voices/sample-languages").json()]
    assert codes == ["en", "hi", "te", "kn"]

    def script(lang: str, voice: str) -> str:
        return str(client.get(f"/v1/voices/sample-script?owner=Sam&lang={lang}&voice={voice}").json()["script"])

    # Hindi verbs follow the voice's gender; Telugu and Kannada don't change.
    assert "Sam की असिस्टेंट बोल रही हूँ" in script("hi", "Kore")
    assert "Sam की असिस्टेंट बोल रहा हूँ" in script("hi", "Puck")
    assert "[" not in script("hi", "Puck")
    assert script("te", "Kore") == script("te", "Puck")
    assert script("kn", "Kore").startswith("ಹಲೋ, ನಾನು Sam ಅವರ")
    assert client.get("/v1/voices/sample-script?lang=xx").json()["error"]["code"] == "unknown_language"


async def test_each_language_is_its_own_recording(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    for lang in ("en", "te", "hi", "en"):
        assert client.get(f"/v1/voices/Kore/sample?owner=Sam&lang={lang}").status_code == 200
    assert voice_agent.voices == ["Kore", "Kore", "Kore"]  # the second "en" came from memory


def test_calls_default_to_kore(client: TestClient, voice_agent: FakeVoiceAgent) -> None:
    with client.websocket_connect("/v1/calls/test") as ws:
        events_until(ws, "turn_complete")
        ws.send_json({"type": "end"})
        ended, _ = events_until(ws, "call_ended")
    assert voice_agent.voices == ["Kore"]
    assert ended[-1]["call"]["voice"] == "Kore"
