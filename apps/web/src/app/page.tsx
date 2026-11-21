import type { ReactNode } from 'react'
import { CallDemo } from '@/components/call-demo'
import { Eyebrow, SiteFooter, SiteHeader } from '@/components/site'
import { WaitlistForm, WaitlistProvider } from '@/components/waitlist'

export default function Home() {
  return (
    <WaitlistProvider>
      <SiteHeader />
      <main>
        <Hero />
        <HowItWorks />
        <ForFamilies />
        <CallIt />
        <Privacy />
        <Faq />
        <FinalCta />
      </main>
      <SiteFooter />
    </WaitlistProvider>
  )
}

function Hero() {
  return (
    <section className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-12 px-4 pt-10 pb-20 sm:px-6 lg:pt-14 lg:pb-24">
      <div className="min-w-0 flex-[1_1_480px]">
        <p className="mb-6 inline-flex items-center gap-2 rounded-full border border-line bg-card py-1.5 pr-3 pl-2 text-sm text-body">
          <span className="rounded-full bg-mint px-2 py-0.5 text-xs font-semibold text-green-deep">
            Soon
          </span>
          Early access on Android
        </p>
        <h1 className="font-display text-[44px] leading-[1.02] font-semibold tracking-[-0.04em] text-balance sm:text-6xl lg:text-[72px] lg:leading-[0.98]">
          Scammers call. Your assistant answers.
        </h1>
        <p className="mt-6 max-w-[540px] text-lg leading-relaxed text-body sm:text-xl">
          Unknown callers reach your assistant first, on your phone or your parents’, even a basic
          one. It talks to them in their language, lets real people through, and alerts you the
          moment a scammer tries.
        </p>
        <div id="join" className="mt-9 max-w-[520px] scroll-mt-24">
          <WaitlistForm />
        </div>
      </div>
      <div className="min-w-0 flex-[1_1_460px]">
        <CallDemo />
      </div>
    </section>
  )
}

function HowItWorks() {
  const steps = [
    {
      title: 'Switch it on in two minutes',
      body: 'A one-time call-forwarding setting on Jio, Airtel, Vi or BSNL. We walk you through it and test it with you. The number stays the same.',
    },
    {
      title: 'Your assistant takes the calls',
      body: 'Calls that go unanswered or declined, and on smartphones every unknown number, reach your assistant. It asks who’s calling and why, in their language.',
    },
    {
      title: 'You stay in control',
      body: 'Real people are put through or leave a message. Scammers get nowhere. You get a one-line summary of every call and an instant alert for anything suspicious.',
    },
  ]
  return (
    <section className="border-y border-line bg-card">
      <div className="mx-auto max-w-[1200px] px-4 py-20 sm:px-6 lg:py-24">
        <Eyebrow>How it works</Eyebrow>
        <h2 className="mt-3 max-w-[720px] font-display text-4xl leading-[1.04] font-semibold tracking-[-0.035em] sm:text-5xl">
          No new number. No new phone.
        </h2>
        <ol className="mt-12 grid gap-5 md:grid-cols-3">
          {steps.map((s, i) => (
            <li key={s.title} className="rounded-3xl bg-paper p-7">
              <span className="flex size-8 items-center justify-center rounded-full bg-ink text-sm font-semibold text-white">
                {i + 1}
              </span>
              <h3 className="mt-5 text-xl font-semibold">{s.title}</h3>
              <p className="mt-2.5 text-base leading-relaxed text-body">{s.body}</p>
            </li>
          ))}
        </ol>
      </div>
    </section>
  )
}

const stats = [
  {
    value: '₹4,005 cr',
    label: 'lost to cyber fraud by senior citizens in India in 2025.',
    source: 'Rajya Sabha reply',
    href: 'https://the420.in/india-women-senior-citizens-cyber-fraud-losses-2025/',
  },
  {
    value: '770 crore',
    label: 'fraud calls made to Indian phones in 2025.',
    source: 'Truecaller',
    href: 'https://www.businesstoday.in/india/story/indians-received-over-4000-crore-spam-calls-in-2025-1189-crore-blocked-report-515765-2026-02-11',
  },
  {
    value: '66%',
    label: 'of unknown calls in India are spam.',
    source: 'Truecaller',
    href: 'https://indiantelevision.com/mam/68-billion-spam-calls-recorded-globally-india-ranks-fifth-truecaller-report/',
  },
]

function ForFamilies() {
  const items: { title: string; body: string; icon: ReactNode }[] = [
    {
      title: 'Works on any phone',
      body: 'Your parents don’t need a smartphone or an app. Forwarding is a network setting, so even a basic phone is covered.',
      icon: (
        <path d="M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM11 18h2" />
      ),
    },
    {
      title: 'Speaks their language',
      body: 'Callers hear a natural voice in the language they speak, and it switches mid-sentence the way people actually talk.',
      icon: <path d="M4 5h9M8.5 3v2M6 5c0 4 3 7 6 8M11 5c-.5 3-3 6-6 7M13 21l4-9 4 9M14.5 18h5" />,
    },
    {
      title: 'Live scam alerts',
      body: 'Fake police, bank, courier or “your son is in trouble” calls are spotted as they happen, and you’re told straight away.',
      icon: <path d="M12 3l8 4v5c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V7zM12 8v5M12 16h.01" />,
    },
    {
      title: 'Real people still get through',
      body: 'The doctor, the courier, an old friend: genuine callers are put through or leave a message nobody misses.',
      icon: (
        <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2" />
      ),
    },
  ]
  return (
    <section className="mx-auto max-w-[1200px] px-4 py-20 sm:px-6 lg:py-24">
      <div className="flex flex-wrap items-end justify-between gap-6">
        <div>
          <Eyebrow>Built for families</Eyebrow>
          <h2 className="mt-3 max-w-[640px] font-display text-4xl leading-[1.04] font-semibold tracking-[-0.035em] sm:text-5xl">
            The people scammers target most are the least protected.
          </h2>
        </div>
        <p className="max-w-[420px] text-lg leading-relaxed text-body">
          Built-in call screening only comes on expensive phones. Your assistant covers everyone in
          the family, on whatever phone they already use.
        </p>
      </div>
      <dl className="mt-12 grid gap-4 rounded-3xl bg-ink p-6 text-white sm:grid-cols-3 sm:p-8">
        {stats.map((s) => (
          <div key={s.label} className="min-w-0">
            <dt className="sr-only">{s.label}</dt>
            <dd className="font-display text-4xl font-semibold tracking-[-0.03em] text-mint-bright sm:text-5xl">
              {s.value}
            </dd>
            <dd className="mt-2 text-[15px] leading-snug text-[#c3d1c8]">
              {s.label}{' '}
              <a
                href={s.href}
                target="_blank"
                rel="noopener noreferrer"
                className="whitespace-nowrap text-[#8fa198] underline hover:text-white"
              >
                {s.source}
              </a>
            </dd>
          </div>
        ))}
      </dl>
      <div className="mt-4 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {items.map((it) => (
          <div key={it.title} className="rounded-3xl border border-line bg-card p-6">
            <svg
              width="26"
              height="26"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#0A7A53"
              strokeWidth="1.8"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              {it.icon}
            </svg>
            <h3 className="mt-4 text-lg font-semibold">{it.title}</h3>
            <p className="mt-2 text-[15px] leading-relaxed text-body">{it.body}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

function CallIt() {
  const chat: { me: boolean; text: string }[] = [
    { me: true, text: 'Did anyone important call Dad today?' },
    {
      me: false,
      text: 'The clinic, to confirm Thursday at 11. I put them through. Two scam calls were blocked.',
    },
    { me: true, text: 'Remind me to pay the electricity bill on Friday.' },
    { me: false, text: 'Done. I’ll remind you at 9 am on Friday.' },
  ]
  return (
    <section className="bg-ink text-white">
      <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-12 px-4 py-20 sm:px-6 lg:py-24">
        <div className="min-w-0 flex-[1_1_400px]">
          <Eyebrow tone="mint">When you call it</Eyebrow>
          <h2 className="mt-3 font-display text-4xl leading-[1.04] font-semibold tracking-[-0.035em] sm:text-5xl">
            And when you need something, just call it.
          </h2>
          <p className="mt-5 max-w-[460px] text-lg leading-relaxed text-[#c3d1c8]">
            Ask what happened on the family’s calls, set reminders and get follow-ups done, just by
            talking. In the language you think in.
          </p>
        </div>
        <div className="flex min-w-0 flex-[1_1_420px] flex-col gap-2.5">
          {chat.map((m) => (
            <p
              key={m.text}
              className={`max-w-[85%] px-4 py-3 text-[15px] leading-snug ${
                m.me
                  ? 'self-end rounded-[16px_16px_4px_16px] bg-white text-ink'
                  : 'self-start rounded-[16px_16px_16px_4px] bg-night text-[#e9f1ec]'
              }`}
            >
              {m.text}
            </p>
          ))}
        </div>
      </div>
    </section>
  )
}

function Privacy() {
  const rules = [
    {
      title: 'Callers always know',
      body: 'It says it’s an AI assistant at the start of every call.',
    },
    {
      title: 'You choose what’s kept',
      body: 'Keep summaries, full transcripts or nothing at all.',
    },
    {
      title: 'Sensitive details never stored',
      body: 'OTPs, card and account numbers are stripped out before anything is saved.',
    },
    {
      title: 'No ads, never sold',
      body: 'Your calls aren’t sold, shared with advertisers or used to show you ads.',
    },
  ]
  return (
    <section className="mx-auto max-w-[1200px] px-4 py-20 sm:px-6 lg:py-24">
      <div className="flex flex-wrap gap-12">
        <div className="min-w-0 flex-[1_1_320px]">
          <Eyebrow>Privacy</Eyebrow>
          <h2 className="mt-3 font-display text-4xl leading-[1.04] font-semibold tracking-[-0.035em] sm:text-5xl">
            Your calls stay yours.
          </h2>
          <p className="mt-5 max-w-[400px] text-lg leading-relaxed text-body">
            An assistant that listens to your family’s calls has to earn it. These are the rules
            it’s built on, with data stored in India.
          </p>
        </div>
        <div className="grid min-w-0 flex-[2_1_520px] gap-3.5 sm:grid-cols-2">
          {rules.map((r) => (
            <div key={r.title} className="rounded-3xl border border-line bg-card p-6">
              <h3 className="text-lg font-semibold">{r.title}</h3>
              <p className="mt-2 text-[15px] leading-relaxed text-body">{r.body}</p>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}

function Faq() {
  const faqs = [
    {
      q: 'Do my parents need a smartphone?',
      a: 'No. Call forwarding is a setting on the mobile network, so it works on any phone, including basic keypad phones. You manage everything from your own app.',
    },
    {
      q: 'Do we need a new phone number?',
      a: 'No. Everyone keeps their number. Calls are forwarded to the assistant only when they aren’t picked up, are declined, or, on a smartphone, come from an unknown number.',
    },
    {
      q: 'Which languages does it speak?',
      a: 'It’s being built for the way India talks, starting with Hindi, English and Telugu, mixed languages included. More Indian languages will follow.',
    },
    {
      q: 'What will it cost?',
      a: 'Early access is free. Later there will be simple plans, with extra minutes you can add only if you need them. Scam calls will never count against your minutes.',
    },
    {
      q: 'When can I try it?',
      a: 'Invites go out in small batches. Join the waitlist and we’ll email you when it’s your turn.',
    },
  ]
  return (
    <section className="border-t border-line bg-card">
      <div className="mx-auto max-w-[760px] px-4 py-20 sm:px-6 lg:py-24">
        <h2 className="font-display text-4xl font-semibold tracking-[-0.03em]">Questions</h2>
        <div className="mt-7">
          {faqs.map((f) => (
            <details key={f.q} className="group border-t border-line py-5 last:border-b">
              <summary className="flex cursor-pointer list-none items-center justify-between gap-4 text-lg font-semibold [&::-webkit-details-marker]:hidden">
                {f.q}
                <span
                  aria-hidden="true"
                  className="text-2xl leading-none text-dim transition-transform group-open:rotate-45"
                >
                  +
                </span>
              </summary>
              <p className="mt-2.5 text-base leading-relaxed text-body">{f.a}</p>
            </details>
          ))}
        </div>
      </div>
    </section>
  )
}

function FinalCta() {
  return (
    <section className="bg-ink text-white">
      <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-8 px-4 py-20 sm:px-6">
        <h2 className="min-w-0 flex-[1_1_460px] font-display text-4xl leading-none font-semibold tracking-[-0.04em] sm:text-[52px]">
          Stop scam calls before they reach your family.
        </h2>
        <div className="min-w-0 max-w-[500px] flex-[1_1_400px]">
          <WaitlistForm tone="dark" compact />
        </div>
      </div>
    </section>
  )
}
