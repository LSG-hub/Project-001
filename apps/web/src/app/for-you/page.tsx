import type { Metadata } from 'next'
import { CallDemo } from '@/components/call-demo'
import { JsonLd } from '@/components/json-ld'
import {
  CallIt,
  Faq,
  FeatureGrid,
  FinalCta,
  Hero,
  HowItWorks,
  icons,
  Privacy,
  setupSteps,
} from '@/components/sections'
import { Eyebrow, SiteFooter, SiteHeader } from '@/components/site'
import { WaitlistProvider } from '@/components/waitlist'
import { faqs, pages } from '@/lib/content'

export const metadata: Metadata = {
  title: pages.forYou.title,
  description: pages.forYou.description,
  alternates: { canonical: pages.forYou.path },
  openGraph: {
    title: pages.forYou.shortTitle,
    description: pages.forYou.description,
    url: pages.forYou.path,
  },
}

export default function ForYouPage() {
  return (
    <WaitlistProvider>
      <SiteHeader />
      <main>
        <JsonLd page="forYou" faq={faqs.forYou} />
        <Hero
          badge="Early access on Android"
          title="Never pick up a call you don’t want to."
          body="For introverts, busy people and anyone who dreads an unknown number. Your assistant answers, finds out what the caller wants, and handles it or sums it up in one line. You reply when you’re ready, by text if you like."
          visual={<CallDemo scenario="personal" />}
        />
        <FeatureGrid
          eyebrow="Built for people who’d rather text"
          title="It talks, so you don’t have to."
          aside="Most calls are small: a delivery at the gate, a sales pitch, a reschedule. Your assistant deals with them, and you only hear about what matters."
          features={[
            {
              title: 'Handles the everyday calls',
              body: 'The delivery finds the gate, the sales call gets a polite no, the clinic’s reminder becomes a note. Most calls end right there.',
              icon: icons.quiet,
            },
            {
              title: 'Your answers, ready to go',
              body: 'Teach it what you’re always asked: gate directions, your working hours, “call me after 6”. It uses them, so you never repeat yourself.',
              icon: icons.list,
            },
            {
              title: 'Type it, it says it',
              body: 'Want to reply yourself without talking? Type your answer during the call, and the assistant says it to the caller.',
              icon: icons.keyboard,
            },
            {
              title: 'Only what matters reaches you',
              body: 'A one-line summary of every call. Your contacts ring as usual, and you can take over any call with one tap.',
              icon: icons.call,
            },
          ]}
        />
        <section className="mx-auto max-w-[1200px] px-4 pb-20 sm:px-6 lg:pb-24">
          <div className="flex flex-wrap items-center justify-between gap-8 rounded-3xl border border-line bg-card p-8 sm:p-10">
            <div className="min-w-0 max-w-[640px] flex-[1_1_420px]">
              <div className="flex items-center gap-3">
                <Eyebrow>Coming later</Eyebrow>
                <span className="rounded-full bg-soft px-2 py-0.5 text-xs font-medium text-dim">
                  Planned
                </span>
              </div>
              <h2 className="mt-3 font-display text-3xl font-semibold tracking-[-0.03em] sm:text-4xl">
                A public number that isn’t your real one.
              </h2>
              <p className="mt-3 text-base leading-relaxed text-body sm:text-lg">
                A separate number to put on forms, marketplace listings and social media. Calls go
                straight to your assistant, and only the ones that matter are put through to you.
                Your real number stays private.
              </p>
            </div>
            <svg
              width="72"
              height="72"
              viewBox="0 0 24 24"
              fill="none"
              stroke="#0A7A53"
              strokeWidth="1.4"
              strokeLinecap="round"
              strokeLinejoin="round"
              aria-hidden="true"
            >
              {icons.hash}
            </svg>
          </div>
        </section>
        <HowItWorks title="Set up once. Then let it ring." steps={setupSteps} />
        <CallIt
          chat={[
            { me: true, text: 'Any calls while I was in the meeting?' },
            {
              me: false,
              text: 'Three. The courier left your parcel with security, a bank sales call was declined, and your landlord wants to visit Saturday.',
            },
            { me: true, text: 'Remind me to reply to the landlord at 6.' },
            { me: false, text: 'Done. I’ll remind you at 6 pm.' },
          ]}
        />
        <Privacy />
        <Faq items={faqs.forYou} />
        <FinalCta title="Let your phone ring. Your assistant has it." />
      </main>
      <SiteFooter />
    </WaitlistProvider>
  )
}
