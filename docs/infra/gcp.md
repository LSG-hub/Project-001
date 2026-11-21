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
