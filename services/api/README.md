# 001 API

The backend for the app and, soon, the phone line. Python 3.13, FastAPI, Gemini Live on Vertex AI, Firestore. Runs on Cloud Run in `asia-south1`.

## Run it

Needs [uv](https://docs.astral.sh/uv/). Locally everything is in memory and the phone line is faked, so no Google Cloud access is needed.

```bash
cd services/api
uv sync
uv run uvicorn project001_api.main:app --reload --port 5302
```

Open http://localhost:5302/docs for the interactive API (off in prod).

Sign in locally: `POST /v1/verifications` with `{"phone": "+919876543210", "method": "voice_code"}`. The fake phone line logs the code (`code=…`) in the server output. Send it to `POST /v1/verifications/{id}/code` and you get a token for `GET /v1/me`.

Checks (CI runs the same):

```bash
uv run ruff check . && uv run ruff format --check . && uv run mypy && uv run lint-imports && uv run pytest
```

Local runs that talk to Gemini use Application Default Credentials. Sign them in to the personal account once: `gcloud auth application-default login`, then `gcloud auth application-default set-quota-project project-001-474105`.

**Test calls** (local only, they spend Gemini credits):

- **In the browser:** open http://localhost:5302/dev/call, press Start call and talk (use headphones). You see live transcripts, every streamed event, and the summary when you hang up. No mic? Untick it and type the caller's lines.
- **Scripted caller:** `uv run python scripts/call_sim.py --list`, then e.g. `uv run python scripts/call_sim.py bank_scam`. macOS voices speak the caller's lines; it prints the transcript and summary, and saves a recording to `recordings/` (git-ignored).
- **One Live turn:** `uv run python scripts/live_smoke.py "Hello? Who is this?"`

A caller's audio must keep streaming, as a real phone line does. Gemini's voice activity detection needs a steady stream to notice the caller has stopped, so the simulator sends faint noise between lines.

## Architecture

A modular monolith: one deployable service, split into modules with hard boundaries.

```
src/project001_api/
  main.py          FastAPI app: middleware, error handling, routers
  container.py     composition root: picks adapters per environment, wires modules
  core/            plain building blocks: settings, logging, errors, phone numbers, auth dependency
  adapters/        the outside world: Gemini Live, Gemini (structured output), Firestore,
                   Firebase Auth tokens, telephony
  modules/
    health/        GET /health
    accounts/      one account per phone number; GET /v1/me
    verification/  proving a number: voice code, missed call; hands out the sign-in token
    context/       about you, calls you're expecting; GET/PUT /v1/context/*
    calls/         a call with the assistant: live audio both ways, transcript, end_call
                   and read_past_call tools, time notes, summary, voice samples; test-call
                   WebSocket and page (dev only)
```

Rules, enforced in CI by import-linter (`lint-imports`):

- **Layers:** `main` → `container` → `modules` → `adapters` → `core`. Nothing imports upwards.
- **Modules are independent.** A module never imports another module. It declares what it needs in its `ports.py` (Python protocols), and `container.py` plugs in the real thing.
- **Modules don't import adapters.** They get them through the container. That's why every module runs in tests with in-memory stores and a fake phone line.

Each module owns its data. Its repository has an in-memory version for tests and a Firestore version for Cloud Run.

Adding a feature: create `modules/<name>/` with its `service.py`, `router.py` (a `build_router(...)` function), a `repository.py` if it stores anything, and a `ports.py` for anything it needs from outside. Then wire it in `container.py` and `main.py`.

## Sign-in

One account per mobile number, stored in E.164 form. Two ways to prove a number, both checked by the phone network, not by the app:

| Method | How | Cost |
|---|---|---|
| `missed_call` (default) | The phone calls our number (`call_to`). The carrier's caller ID proves the SIM. The telephony webhook calls `confirm_inbound_call`; the app polls `GET /v1/verifications/{id}` | Free both ways |
| `voice_code` | We call the number and read out a 6-digit code | One short outbound call |

Limits: 10 minutes per attempt, 5 wrong codes, 5 attempts per number per hour. Codes are stored only as hashes. Once a number is verified, the API returns a Firebase Auth custom token, exactly once. The app signs in to Firebase Auth with it and sends the ID token as `Authorization: Bearer …`.

Until the telephony provider is connected, prod refuses voice codes rather than pretending, and missed calls can't arrive yet.

## What the assistant knows on a call

At the start of every call, `CallService.prepare` builds the assistant's instructions: the screening rules (`calls/prompts.py: screener`) plus a brief (`call_brief`) with three layers. See it live at `GET /v1/calls/context?caller=+91…` or in the test page's "Sent to Gemini at call start" panel.

| Layer | Stored in (Firestore) | Injected as |
|---|---|---|
| About you | `profiles/{owner_id}`: name, about, timezone | "From <you>, about themselves:" (your words are instructions) |
| Calls you're expecting | `expected_calls/{id}`: text, expires_at, met_by_call | "From <you>, calls they're expecting:" with ids; the summary marks the matching one done |
| This caller's history | `calls/{id}`: owner_id, caller_number, turns, summary | The last 5 summaries with ids; the `read_past_call` tool opens a full transcript, only for this caller's own calls |

Past calls are marked as information, never instructions, so a caller can't plant instructions for a later call. The brief also gives the current local time.

During the call, silent notes at 5, 10 and 15 minutes (`TIME_NOTES`) nudge the assistant towards a graceful wrap-up. They're added with `send_client_content(turn_complete=False)`, so the assistant reads them without replying and the caller never hears them.

API: `GET/PUT /v1/context/profile`, `GET/POST /v1/context/expected`, `DELETE /v1/context/expected/{id}`, `GET /v1/calls`, `GET /v1/calls/{id}`. They need sign-in (`Authorization: Bearer …`). Locally, requests without a token act as `dev-owner`.

The local preview config runs on the Firestore `development` database (`API_STORE=firestore`), so memory survives restarts. Browse it at https://console.firebase.google.com/project/project-001-474105/firestore/databases/development/data.

## Configuration

Environment variables, all prefixed `API_` (see `core/settings.py`):

| Variable | Local | Dev | Prod |
|---|---|---|---|
| `API_ENV` | `local` | `dev` | `prod` |
| `API_STORE` | `memory` | `firestore` | `firestore` |
| `API_FIRESTORE_DATABASE` | n/a | `development` | `(default)` |
| `API_AUTH` | `fake` | `firebase` | `firebase` |
| `API_LIVE_MODEL` | `gemini-3.8-live` | same | same |

## Deploys

`.github/workflows/deploy-api.yml`. A merge into `dev` deploys Cloud Run `api-dev`; a merge into `main` deploys `api`. Each deploy re-applies the size and settings. See [docs/infra/gcp.md](../../docs/infra/gcp.md).
