"""The composition root: the one place that picks adapters and wires modules together.

Modules never import each other or the adapters; they declare what they need
(see each module's ports) and this file plugs the pieces in.
"""

from dataclasses import dataclass
from datetime import timedelta
from pathlib import Path

from project001_api.adapters.auth_tokens import FakeTokens, FirebaseTokens
from project001_api.adapters.firestore import make_firestore_client
from project001_api.adapters.gemini_live import GeminiLive
from project001_api.adapters.gemini_text import GeminiStructured
from project001_api.adapters.telephony import FakeTelephony
from project001_api.adapters.vertex import LazyGenaiClient
from project001_api.core.clock import Clock, SystemClock
from project001_api.core.settings import Env, Settings
from project001_api.modules.accounts.repository import (
    AccountRepository,
    FirestoreAccountRepository,
    InMemoryAccountRepository,
)
from project001_api.modules.accounts.service import AccountService
from project001_api.modules.calls.ports import StructuredModel, VoiceAgent
from project001_api.modules.calls.repository import CallRepository, FirestoreCallRepository, InMemoryCallRepository
from project001_api.modules.calls.samples import VoiceSamples
from project001_api.modules.calls.service import CallService
from project001_api.modules.context.repository import (
    ContextRepository,
    FirestoreContextRepository,
    InMemoryContextRepository,
)
from project001_api.modules.context.service import ContextService
from project001_api.modules.verification.repository import (
    FirestoreVerificationRepository,
    InMemoryVerificationRepository,
    VerificationRepository,
)
from project001_api.modules.verification.service import Limits, VerificationService


@dataclass
class Container:
    settings: Settings
    tokens: FakeTokens | FirebaseTokens
    accounts: AccountService
    verification: VerificationService
    context: ContextService
    calls: CallService
    samples: VoiceSamples


def build_container(
    settings: Settings,
    *,
    clock: Clock | None = None,
    telephony: FakeTelephony | None = None,
    voice_agent: VoiceAgent | None = None,
    structured_model: StructuredModel | None = None,
    time_notes: list[tuple[float, str]] | None = None,
) -> Container:
    clock = clock or SystemClock()

    account_repo: AccountRepository
    verification_repo: VerificationRepository
    call_repo: CallRepository
    context_repo: ContextRepository
    if settings.store == "firestore":
        db = make_firestore_client(settings.gcp_project, settings.firestore_database)
        account_repo = FirestoreAccountRepository(db)
        verification_repo = FirestoreVerificationRepository(db)
        call_repo = FirestoreCallRepository(db)
        context_repo = FirestoreContextRepository(db)
    else:
        account_repo = InMemoryAccountRepository()
        verification_repo = InMemoryVerificationRepository()
        call_repo = InMemoryCallRepository()
        context_repo = InMemoryContextRepository()

    tokens: FakeTokens | FirebaseTokens = (
        FirebaseTokens(settings.gcp_project, settings.firebase_signer) if settings.auth == "firebase" else FakeTokens()
    )
    # Until the real phone line exists, prod refuses voice codes instead of pretending.
    telephony = telephony or FakeTelephony(pretend=settings.env is not Env.PROD)

    # One Vertex AI client, created on first use.
    genai_client = LazyGenaiClient(settings.gcp_project, settings.gcp_region)
    voice_agent = voice_agent or GeminiLive(genai_client, settings.live_model, settings.live_voice)
    structured_model = structured_model or GeminiStructured(genai_client, settings.summary_model)

    accounts = AccountService(account_repo, clock)
    verification = VerificationService(
        verification_repo,
        telephony,
        accounts,
        tokens,
        clock,
        inbound_number=settings.inbound_number,
        limits=Limits(
            ttl=timedelta(seconds=settings.verification_ttl_seconds),
            max_attempts=settings.verification_max_attempts,
            max_starts_per_hour=settings.verification_max_starts_per_hour,
        ),
        accept_any_code=settings.verification_accept_any_code and not settings.is_prod,
    )
    context = ContextService(context_repo, clock)
    calls = CallService(
        call_repo,
        voice_agent,
        structured_model,
        context,
        clock,
        default_voice=settings.live_voice,
        time_notes=time_notes,
    )
    sample_dir = Path(settings.voice_sample_dir) if settings.voice_sample_dir and settings.dev_tools_enabled else None
    samples = VoiceSamples(voice_agent, sample_dir=sample_dir, default_voice=settings.live_voice)
    return Container(
        settings=settings,
        tokens=tokens,
        accounts=accounts,
        verification=verification,
        context=context,
        calls=calls,
        samples=samples,
    )
