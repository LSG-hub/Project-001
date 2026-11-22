from collections.abc import Iterator

import pytest
from fastapi.testclient import TestClient

from project001_api.adapters.telephony import FakeTelephony
from project001_api.container import Container, build_container
from project001_api.core.clock import FakeClock
from project001_api.core.settings import Env, Settings
from project001_api.main import create_app
from tests.fakes import FakeStructuredModel, FakeVoiceAgent


class RecordingTelephony(FakeTelephony):
    """Keeps the codes it would have read out, so tests can type them in."""

    def __init__(self, *, pretend: bool = True) -> None:
        super().__init__(pretend=pretend)
        self.codes: dict[str, str] = {}

    async def speak_code(self, to: str, code: str, language: str | None) -> None:
        self.codes[to] = code


@pytest.fixture
def settings() -> Settings:
    return Settings(env=Env.LOCAL, store="memory", auth="fake", inbound_number="+919999900001", voice_sample_dir=None)


@pytest.fixture
def clock() -> FakeClock:
    return FakeClock()


@pytest.fixture
def telephony() -> RecordingTelephony:
    return RecordingTelephony()


@pytest.fixture
def voice_agent() -> FakeVoiceAgent:
    return FakeVoiceAgent()


@pytest.fixture
def structured_model() -> FakeStructuredModel:
    return FakeStructuredModel()


@pytest.fixture
def container(
    settings: Settings,
    clock: FakeClock,
    telephony: RecordingTelephony,
    voice_agent: FakeVoiceAgent,
    structured_model: FakeStructuredModel,
) -> Container:
    return build_container(
        settings, clock=clock, telephony=telephony, voice_agent=voice_agent, structured_model=structured_model
    )


@pytest.fixture
def client(settings: Settings, container: Container) -> Iterator[TestClient]:
    with TestClient(create_app(settings, container)) as c:
        yield c
