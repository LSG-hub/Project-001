# Build with Google Gemini — Kickoff / Explainer Session

**Hackathon:** Build with Google Gemini (YourStory × Google Cloud × Hack2skill)
**Format:** Free, virtual, for **working professionals across India**. Teams of **1–4**.
**Session:** ~38 min. Host: Daman Sharma (Hack2skill). Speaker: Manish Kumar, Customer Engineer / AI architect, Google Cloud.
**Notes saved:** 2026-11-21 (session date not stated; it happened before the 26 Nov hands-on session)
**Support:** spoken as "support plus buildwithgemini at hack2skill.com", probably `support+buildwithgemini@hack2skill.com` (check the dashboard before emailing)

---

## 1. Key dates

| Date        | What                                                                                              |
| ----------- | ------------------------------------------------------------------------------------------------- |
| **26 Nov**  | Hands-on session (recording of the kickoff is on the dashboard)                                   |
| **3 Dec**   | **Registration + idea submission close.** Ideas can be edited/changed only until then.            |
| till 4 Dec  | Idea evaluation (**rolling**; selected teams get an email, said to be ~3–4 days after submitting) |
| **6 Dec**   | **Prototype submission deadline** (only teams whose idea was selected)                            |
| till 13 Dec | Prototype evaluation                                                                              |
| 15 Dec      | Top teams announced                                                                               |
| 18–19 Dec   | Virtual demo day (2 days)                                                                         |

Because evaluation is rolling and the prototype is due 6 Dec, **submitting the idea early means more build time.**

## 2. Prizes and perks

- Winner **₹3,00,000** · 1st runner-up **₹2,00,000** · 2nd runner-up **₹1,25,000**
- Consolation: 5 teams, up to ₹35,000 (as spoken; check the site)
- **$100 Google Cloud credits** to build the prototype (and new GCP accounts get $300 free credit)
- E-certificate for every submitted prototype, office hours and guided sessions with Google Cloud + Hack2skill

---

## 3. The core requirement: Gemini Live

The point of the hackathon is to use **Gemini Live**, Google's **single speech-to-speech model** (audio in, audio out), instead of the old three-model chain STT → LLM → TTS (three models = orchestration, capacity and latency headaches).

"India is a voice-first market": people don't like typing.

**Technical expectations (from the functional / non-functional boxes on the slides):**

- **Latency:** responses within ~1–1.5 s; Gemini Live is usually sub-second.
- **Vernacular fluency + language switching mid-conversation** (start in English, switch to Hindi, the agent follows).
- **Tool calling:** it must _do_ things, not just talk. Gemini Live supports up to 128 function calls; aim for ~5–10 tools in a demo.
- **Grounded output:** call a RAG store / source of truth, not free-form answers.
- **Seamless interruption (barge-in):** the bot stops immediately when the user talks.
- **VAD (voice activity detection):** Gemini Live has a built-in _semantic_ VAD (waits when you pause mid-sentence). Silero is the common external option.
- **WebSocket session:** Live is one continuous session. Use **context update** to change system instructions on the fly (e.g. KYC persona → FAQ persona).
- **Cost per call:** Indian businesses are targeting **₹1–2 per call minute**. Keep system prompts small; run an **orchestrator / step-function outside the model** that loads small "prompt cards" per sub-journey (e.g. showroom call: consent → products → pin code → nearest showroom → appointment). Cost = input/output text + audio, so context engineering controls cost.
- **Noise isolation** in front of the model for real-world calls (bus honks, Bengaluru traffic): Krisp, RNNoise, DeepFilterNet v2.

---

## 4. The four problem statements (guidance only; your own idea is fine)

You can also combine them into one "nirvana" agent.

1. **Voice-powered enterprise knowledge & decision agent**
   Voice bot grounded in enterprise data. Examples: a tutor that knows the student's history and subjects; a CRM agent ("what's the growth projection for my accounts next two quarters?") that picks tools, pulls data, summarises and speaks it.
2. **Voice-driven agentic workflow automation**
   Talk to perform tasks: book a ticket, run a business workflow. Manish's favourite: Gemini Live inside **Antigravity** (Google's coding harness) so you speak instructions to the coding agent instead of typing.
3. **Frontline voice agents (customer support)**
   Latency-sensitive, grounded, interruption handling, noise cancellation, low cost per call. Example: a delivery rider stuck in traffic calls support and a Gemini Live agent handles it.
4. **Open innovation (non-desk workers / anything real-world)**
   Example: Manish's own "Jarvis" on his Mac that opens Terminal, Finder or Chrome tabs by voice.

**Use cases Manish has seen with customers:**

- Gemini Live in hardware: earbuds companion, camera glasses describing what's in front of you.
- "Where is my order?" support bot (looks up user ID → orders → tracking).
- Voice shopping: search, show options, virtual try-on via camera, add to cart, all the way to the payment page by voice.
- Voice tutor for school kids (e.g. explain Newton's laws simply).
- Flight booking via Google Flights APIs → choose → payment page.

---

## 5. Q&A highlights

- **Billing account / free tier?** A new GCP account gives $300 credit, plenty for this.
- **Is the idea round graded?** Yes, rolling evaluation; only selected ideas move to the prototype phase (due 6 Dec).
- **Must every part be Gemini Enterprise / Google?** No. **The voice model must be Gemini Live**, and a Google compute platform like **Cloud Run** is expected. Using more Google services earns "brownie points": Vertex AI, Google's vector DB / built-in RAG, **ADK** for orchestration.
- **Open-source instead of GCP for the pipeline?** Fine (CrewAI, LangGraph, LlamaIndex, etc.), as long as the model is Gemini Live.
- **Language / tools?** Any: Python, Go, TypeScript, etc. You're building an _agent_ (model + context + tools), not a model.
- **Can I change my idea?** Only until 3 Dec.
- **How do I validate an idea?** No framework needed; look at voice apps on Google/Twitter, do discovery, validate with your team.

## 6. What to submit (suggested by Manish)

- Vibe-code it, ideally in **Antigravity** with Google's latest models (names garbled in the transcript as "neon and carbon").
- Public **GitHub repo**.
- **Deploy on Cloud Run** and share the live link.
- A few **slides**: the problem, how you solve it, what you built.
- A **demo video** (recommended).

---

## 7. Checklist

- [ ] Register + submit idea **early** (rolling evaluation; hard close 3 Dec)
- [ ] Attend the 26 Nov hands-on session
- [ ] Get a GCP account + credits; confirm Gemini Live API access
- [ ] Architecture: Gemini Live (WebSocket) + external orchestrator with prompt cards + 5–10 tools + grounding (RAG) + noise suppression
- [ ] Show: sub-second replies, barge-in, language switching, tool calls, cost-per-minute estimate
- [ ] Deploy on Cloud Run; public GitHub repo
- [ ] Slides + demo video
- [ ] Prototype in by **6 Dec**
