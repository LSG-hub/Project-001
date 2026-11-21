import Link from 'next/link'
import type { ReactNode } from 'react'
import { Badge, Eyebrow } from './site'
import { WaitlistForm } from './waitlist'

const h2 =
  'font-display text-4xl leading-[1.04] font-semibold tracking-[-0.035em] text-balance sm:text-5xl'

export function Hero({
  badge,
  title,
  body,
  visual,
}: {
  badge: string
  title: string
  body: string
  visual: ReactNode
}) {
  return (
    <section className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-12 px-4 pt-8 pb-20 sm:px-6 lg:pt-12 lg:pb-24">
      <div className="min-w-0 flex-[1_1_480px]">
        <Badge>{badge}</Badge>
        <h1 className="font-display text-[44px] leading-[1.02] font-semibold tracking-[-0.04em] text-balance sm:text-6xl lg:text-[72px] lg:leading-[0.98]">
          {title}
        </h1>
        <p className="mt-6 max-w-[540px] text-lg leading-relaxed text-body sm:text-xl">{body}</p>
        <div id="join" className="mt-9 max-w-[520px] scroll-mt-24">
          <WaitlistForm />
        </div>
      </div>
      <div className="min-w-0 flex-[1_1_460px]">{visual}</div>
    </section>
  )
}

export type Feature = { title: string; body: string; icon: ReactNode; tag?: string }

export function FeatureGrid({
  eyebrow,
  title,
  aside,
  features,
  children,
}: {
  eyebrow: string
  title: string
  aside?: string
  features: Feature[]
  children?: ReactNode
}) {
  return (
    <section className="mx-auto max-w-[1200px] px-4 py-20 sm:px-6 lg:py-24">
      <div className="flex flex-wrap items-end justify-between gap-6">
        <div>
          <Eyebrow>{eyebrow}</Eyebrow>
          <h2 className={`mt-3 max-w-[660px] ${h2}`}>{title}</h2>
        </div>
        {aside && <p className="max-w-[420px] text-lg leading-relaxed text-body">{aside}</p>}
      </div>
      {children}
      <div className="mt-6 grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {features.map((f) => (
          <div key={f.title} className="rounded-3xl border border-line bg-card p-6">
            <div className="flex items-center justify-between">
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
                {f.icon}
              </svg>
              {f.tag && (
                <span className="rounded-full bg-soft px-2 py-0.5 text-xs font-medium text-dim">
                  {f.tag}
                </span>
              )}
            </div>
            <h3 className="mt-4 text-lg font-semibold">{f.title}</h3>
            <p className="mt-2 text-[15px] leading-relaxed text-body">{f.body}</p>
          </div>
        ))}
      </div>
    </section>
  )
}

export const icons = {
  phone: (
    <path d="M8 3h8a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2zM11 18h2" />
  ),
  language: <path d="M4 5h9M8.5 3v2M6 5c0 4 3 7 6 8M11 5c-.5 3-3 6-6 7M13 21l4-9 4 9M14.5 18h5" />,
  shield: <path d="M12 3l8 4v5c0 4.5-3.2 8.2-8 9-4.8-.8-8-4.5-8-9V7zM12 8v5M12 16h.01" />,
  call: (
    <path d="M5 4h4l2 5-2.5 1.5a11 11 0 0 0 5 5L15 13l5 2v4a2 2 0 0 1-2 2A16 16 0 0 1 3 6a2 2 0 0 1 2-2" />
  ),
  chat: <path d="M4 5h16v11H9l-5 4zM8 9h8M8 12h5" />,
  keyboard: <path d="M3 6h18v12H3zM7 10h.01M11 10h.01M15 10h.01M7 14h10" />,
  list: <path d="M9 6h11M9 12h11M9 18h11M4 6h.01M4 12h.01M4 18h.01" />,
  quiet: <path d="M11 5L6 9H3v6h3l5 4zM16 9l5 6M21 9l-5 6" />,
  hash: <path d="M5 9h14M5 15h14M10 4L8 20M16 4l-2 16" />,
}

export function HowItWorks({
  title,
  steps,
}: {
  title: string
  steps: { title: string; body: string }[]
}) {
  return (
    <section className="border-y border-line bg-card">
      <div className="mx-auto max-w-[1200px] px-4 py-20 sm:px-6 lg:py-24">
        <Eyebrow>How it works</Eyebrow>
        <h2 className={`mt-3 max-w-[720px] ${h2}`}>{title}</h2>
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

export const setupSteps = [
  {
    title: 'Switch it on in two minutes',
    body: 'A one-time call-forwarding setting on Jio, Airtel, Vi or BSNL. We walk you through it and test it with you. Your number stays the same.',
  },
  {
    title: 'Your assistant takes the calls',
    body: 'Calls you don’t pick up or decline, and on smartphones every unknown number, reach your assistant. It asks who’s calling and why, in their language.',
  },
  {
    title: 'You stay in control',
    body: 'Real people get through or leave a message. Spam gets a polite no. You get a one-line summary of every call, and an instant alert if anything looks wrong.',
  },
]

export function CallIt({ chat }: { chat: { me: boolean; text: string }[] }) {
  return (
    <section className="bg-ink text-white">
      <div className="mx-auto flex max-w-[1200px] flex-wrap items-center gap-12 px-4 py-20 sm:px-6 lg:py-24">
        <div className="min-w-0 flex-[1_1_400px]">
          <Eyebrow tone="mint">When you call it</Eyebrow>
          <h2 className="mt-3 font-display text-4xl leading-[1.04] font-semibold tracking-[-0.035em] sm:text-5xl">
            And when you need something, just call it.
          </h2>
          <p className="mt-5 max-w-[460px] text-lg leading-relaxed text-[#c3d1c8]">
            Ask what you missed, set reminders and get follow-ups done, just by talking, in the
            language you think in.
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

export function Privacy() {
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
          <h2 className={`mt-3 ${h2}`}>Your calls stay yours.</h2>
          <p className="mt-5 max-w-[400px] text-lg leading-relaxed text-body">
            An assistant that listens to your calls has to earn it. These are the rules it’s built
            on, with data stored in India.
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

export function Faq({ items }: { items: { q: string; a: string }[] }) {
  return (
    <section className="border-t border-line bg-card">
      <div className="mx-auto max-w-[760px] px-4 py-20 sm:px-6 lg:py-24">
        <h2 className="font-display text-4xl font-semibold tracking-[-0.03em]">Questions</h2>
        <div className="mt-7">
          {items.map((f) => (
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

export function FinalCta({ title }: { title: string }) {
  return (
    <section className="bg-ink text-white">
      <div className="mx-auto flex max-w-[1200px] flex-wrap items-center justify-between gap-8 px-4 py-20 sm:px-6">
        <h2 className="min-w-0 flex-[1_1_460px] font-display text-4xl leading-none font-semibold tracking-[-0.04em] text-balance sm:text-[52px]">
          {title}
        </h2>
        <div className="min-w-0 max-w-[500px] flex-[1_1_400px]">
          <WaitlistForm tone="dark" compact />
        </div>
      </div>
    </section>
  )
}

export function AudienceCards() {
  const cards = [
    {
      href: '/for-you',
      eyebrow: 'For you',
      title: 'For anyone who’d rather not pick up.',
      body: 'Deliveries, unknown numbers, sales calls, the landlord. It answers, sorts it out or takes a message, and you reply when you’re ready, by text if you like.',
      cta: 'How it works for you',
    },
    {
      href: '/families',
      eyebrow: 'For families',
      title: 'For parents who get the scary calls.',
      body: 'Fake police, bank and “your son is in trouble” calls stop at the assistant, and you get an alert. It works on basic keypad phones too.',
      cta: 'How it protects your family',
    },
  ]
  return (
    <section className="mx-auto max-w-[1200px] px-4 py-20 sm:px-6 lg:py-24">
      <Eyebrow>Who it’s for</Eyebrow>
      <h2 className={`mt-3 max-w-[720px] ${h2}`}>One assistant, two jobs.</h2>
      <div className="mt-12 grid gap-5 md:grid-cols-2">
        {cards.map((c) => (
          <Link
            key={c.href}
            href={c.href}
            className="group flex flex-col rounded-3xl border border-line bg-card p-8 transition-colors hover:border-line-strong"
          >
            <Eyebrow>{c.eyebrow}</Eyebrow>
            <h3 className="mt-3 font-display text-3xl font-semibold tracking-[-0.03em]">
              {c.title}
            </h3>
            <p className="mt-3 text-base leading-relaxed text-body">{c.body}</p>
            <span className="mt-6 inline-flex items-center gap-1.5 font-semibold text-green">
              {c.cta}
              <span aria-hidden="true" className="transition-transform group-hover:translate-x-0.5">
                →
              </span>
            </span>
          </Link>
        ))}
      </div>
    </section>
  )
}
