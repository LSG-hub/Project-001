"""Configuration, read from environment variables prefixed with API_.

Local runs need no variables: everything defaults to in-memory stores and fake
adapters. Cloud Run sets API_ENV and the store/adapter switches per environment.
"""

from enum import StrEnum
from functools import lru_cache
from typing import Literal

from pydantic_settings import BaseSettings, SettingsConfigDict


class Env(StrEnum):
    LOCAL = "local"
    DEV = "dev"
    PROD = "prod"


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="API_", env_file=".env", extra="ignore")

    env: Env = Env.LOCAL
    log_level: str = "INFO"

    # Google Cloud
    gcp_project: str = "project-001-474105"
    gcp_region: str = "asia-south1"

    # Storage: "memory" for local runs and tests, "firestore" on Cloud Run.
    store: Literal["memory", "firestore"] = "memory"
    # "development" for dev, "(default)" for prod.
    firestore_database: str = "development"

    # Gemini Live on Vertex AI, and the model that writes call summaries.
    live_model: str = "gemini-3.8-live"
    # The assistant's voice when a call doesn't choose one (see core/voice.py VOICES).
    live_voice: str | None = "Kore"
    summary_model: str = "gemini-3.5-flash"

    # Developer tools: the test-call page and unauthenticated call endpoints. They spend
    # Gemini credits, so they default to on only for local runs.
    dev_tools: bool | None = None
    # Test calls from the app (signed in). On by default only locally; dev turns them on.
    # Never on prod.
    test_calls: bool | None = None
    # Test calls and unauthenticated dev requests act as this account.
    dev_owner_id: str = "dev-owner"
    # Where recorded voice samples are kept (local dev); unset keeps them in memory only.
    voice_sample_dir: str | None = "recordings/voices"

    # Sign-in tokens: "fake" locally, "firebase" on Cloud Run (Firebase Auth custom tokens).
    auth: Literal["fake", "firebase"] = "fake"
    # Service account that signs Firebase custom tokens on Cloud Run (IAM signBlob).
    firebase_signer: str | None = None

    # Telephony. Only "fake" exists until the virtual number is live.
    telephony: Literal["fake"] = "fake"
    # Our virtual number: users give it a missed call to verify. Placeholder for now.
    inbound_number: str = "+919999900001"

    # Phone verification limits.
    verification_ttl_seconds: int = 600
    verification_max_attempts: int = 5
    verification_max_starts_per_hour: int = 5
    # Dev only, until the phone line exists: any 6-digit voice code signs you in.
    # Ignored on prod.
    verification_accept_any_code: bool = False

    @property
    def is_prod(self) -> bool:
        return self.env is Env.PROD

    @property
    def test_calls_enabled(self) -> bool:
        if self.is_prod:
            return False
        return self.test_calls if self.test_calls is not None else self.env is Env.LOCAL

    @property
    def dev_tools_enabled(self) -> bool:
        if self.is_prod:
            return False
        return self.dev_tools if self.dev_tools is not None else self.env is Env.LOCAL


@lru_cache
def get_settings() -> Settings:
    return Settings()
