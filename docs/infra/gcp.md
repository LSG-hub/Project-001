# Google Cloud setup

| What                | Value                                                                               |
| ------------------- | ----------------------------------------------------------------------------------- |
| GCP project         | `project-001-474105` ("Project 001")                                                |
| Account             | personal Google account (gcloud configuration `personal`), never the office account |
| Billing             | My Real Billing Account (ID kept in `docs/private/accounts.md`, git-ignored)          |
| Firebase            | added to the same project                                                           |
| Website             | `001.sreenivasgurram.com` → Firebase Hosting site `project-001-474105.web.app`. GoDaddy CNAME `001` added 2026-11-21 |

## Enabled APIs (added 2026-11-21)

Cloud Run, Vertex AI (`aiplatform`), Artifact Registry, Cloud Build, Firebase, Firebase Hosting, Billing Budgets, Secret Manager.

## Money

- The billing account holds Google Developer Program credits that cover all of Google Cloud. Google uses them before charging the account balance, soonest-expiring first.
- Use Gemini through **Vertex AI**, not an AI Studio API key. AI Studio has its own prepaid wallet, and it is empty.
- Budget "Project 001 monthly": ₹1,000/month. It counts spend **before** credits, so it shows how fast credits are being used. Email alerts go out at 50%, 90% and 100%. A budget only alerts; it never stops services.

## Hosting plan (decided 2026-11-21)

| Piece | Runs on | Address |
|---|---|---|
| Website (Next.js) | Cloud Run, `asia-south1` (Mumbai) | `001.sreenivasgurram.com`, via a Firebase Hosting rewrite to the Cloud Run service |
| Voice backend (FastAPI + WebSockets) | Cloud Run, `asia-south1` | its own `*.run.app` URL. Firebase Hosting rewrites time out at 60 s, so voice sockets can't go through them |
| Data | Firestore | — |
| Gemini Live | Vertex AI `gemini-3.8-live`, `asia-south1` | — |

Why: no load balancer, because one costs about ₹1,600/month, more than a month of credits. Cloud Run domain mapping is preview-only and not offered in Mumbai. Firebase Hosting is only the domain, SSL and CDN layer, at free-tier cost. Firebase is billed on the same Cloud billing account and the Developer Program credits cover it.

Notes:
- Live API connections last about 10 minutes, so the backend must use session resumption for longer calls.
- `gemini-live-2.5-flash-native-audio` retires on 2026-12-13. Don't build on it.

## Useful commands

```bash
gcloud config configurations activate personal
gcloud config set project project-001-474105
```

## Environments and release flow (set up 2026-11-21)

| | Dev | Prod |
|---|---|---|
| Branch | `dev` (default branch) | `main` |
| Address | https://dev.001.sreenivasgurram.com (noindex) | https://001.sreenivasgurram.com |
| Firebase Hosting site | `project-001-474105-dev` (target `dev`) | `project-001-474105` (target `prod`) |
| Cloud Run service | `web-dev` | `web` |
| Firestore database | `development` | `(default)` |

**Flow:** feature branch → PR into `dev` → merge (auto-deploys dev) → PR `dev` → `main` → merge (auto-deploys prod).

**Rules:**
- `main` only accepts PRs from `dev` (the `source-is-dev` check).
- Both branches require PRs and a passing `lint-test-build` check.
- Force pushes and branch deletion are blocked.

**Deploys** (`.github/workflows/deploy-web.yml`):
- Run on pushes to `dev` and `main` that touch the website.
- GitHub signs in through Workload Identity Federation: pool `github`, provider `lsg-hub`, limited to `LSG-hub/Project-001` on `refs/heads/dev` and `refs/heads/main`. It acts as `github-deployer`, which has `run.developer`, `artifactregistry.writer` on repo `web`, and `serviceAccountUser` on `web-runtime`. No keys are stored in GitHub.
- Firebase Hosting config and Firestore rules are deployed by hand when they change: `firebase deploy --only hosting:dev,hosting:prod,firestore:rules`.
