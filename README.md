# Project 001

An AI call assistant that protects families from scam calls. Unknown callers reach the assistant first, on your phone or your parents' (even a basic phone). It talks to them in their language, puts real people through, and alerts you when a scammer tries. You can also call it to get things done. Built on Gemini Live.

**Live:** https://001.sreenivasgurram.com. This is a working name and a temporary address; the product will get its own name and domain later.

## Layout

| Path | What |
|---|---|
| `apps/web` | Website and waitlist (Next.js 16, Tailwind CSS 4). Runs on Cloud Run behind Firebase Hosting |
| `apps/android` | Android app (planned, Kotlin) |
| `services/voice` | Voice backend on Cloud Run: Gemini Live, the phone line, WhatsApp (planned) |
| `brand/` | Logo and brand assets |
| `docs/` | Research (competitors, scams), pricing, hackathon notes, infrastructure |

## Develop

```bash
npm install
npm run dev:web
```

Open http://localhost:5301. In development, waitlist sign-ups are saved to `apps/web/.data/waitlist.json` (git-ignored). Set `WAITLIST_STORE=firestore` or `FIRESTORE_EMULATOR_HOST` to use Firestore instead.

```bash
npm test -w @project001/web
```

## Deploy the website

The site runs as a container on Cloud Run (`web`, asia-south1). Firebase Hosting rewrites every path on 001.sreenivasgurram.com to it, and waitlist sign-ups go to Firestore (asia-south1).

```bash
export CLOUDSDK_ACTIVE_CONFIG_NAME=personal
gcloud builds submit --config cloudbuild.web.yaml --project project-001-474105 --region asia-south1
gcloud run deploy web --project project-001-474105 --region asia-south1 \
  --image asia-south1-docker.pkg.dev/project-001-474105/web/web:latest
firebase deploy --only hosting,firestore:rules --project project-001-474105   # only when firebase.json or rules change
```

The Firebase project is `project-001-474105`. See [docs/infra/gcp.md](docs/infra/gcp.md).

## License

Dual-licensed:
- **Open source:** [GNU AGPL-3.0](LICENSE). Free to use, modify and self-host. If you run a modified version as a network service, you must publish your source under the AGPL.
- **Commercial licence:** for closed-source or proprietary use. See [COMMERCIAL.md](COMMERCIAL.md).

Contributions require a short [CLA](CLA.md); see [CONTRIBUTING.md](CONTRIBUTING.md).
