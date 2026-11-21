import type { Metadata } from 'next'
import { CallDemo } from '@/components/call-demo'
import { JsonLd } from '@/components/json-ld'
import {
  Faq,
  FeatureGrid,
  FinalCta,
  Hero,
  HowItWorks,
  icons,
  Privacy,
  setupSteps,
} from '@/components/sections'
import { SiteFooter, SiteHeader } from '@/components/site'
import { WaitlistProvider } from '@/components/waitlist'
import { faqs, pages } from '@/lib/content'

export const metadata: Metadata = {
  title: pages.families.title,
  description: pages.families.description,
  alternates: { canonical: pages.families.path },
  openGraph: {
    title: pages.families.shortTitle,
    description: pages.families.description,
    url: pages.families.path,
  },
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

export default function FamiliesPage() {
  return (
    <WaitlistProvider>
      <SiteHeader />
      <main>
        <JsonLd page="families" faq={faqs.families} />
        <Hero
          badge="Early access on Android"
          title="Scammers call. Your assistant answers."
          body="Unknown callers reach your assistant first, on your phone or your parents’, even a basic one. It talks to them in their language, lets real people through, and alerts you the moment a scammer tries."
          visual={<CallDemo scenario="family" />}
        />
        <FeatureGrid
          eyebrow="Built for families"
          title="The people scammers target most are the least protected."
          aside="Built-in call screening only comes on expensive phones. Your assistant covers everyone in the family, on whatever phone they already use."
          features={[
            {
              title: 'Works on any phone',
              body: 'Your parents don’t need a smartphone or an app. Forwarding is a network setting, so even a basic phone is covered.',
              icon: icons.phone,
            },
            {
              title: 'Speaks their language',
              body: 'Callers hear a natural voice in the language they speak, and it switches mid-sentence the way people actually talk.',
              icon: icons.language,
            },
            {
              title: 'Live scam alerts',
              body: 'Fake police, bank, courier or “your son is in trouble” calls are spotted as they happen, and you’re told straight away.',
              icon: icons.shield,
            },
            {
              title: 'Real people still get through',
              body: 'The doctor, the courier, an old friend: genuine callers are put through or leave a message nobody misses.',
              icon: icons.call,
            },
          ]}
        >
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
        </FeatureGrid>
        <HowItWorks title="No new number. No new phone." steps={setupSteps} />
        <Privacy />
        <Faq items={faqs.families} />
        <FinalCta title="Stop scam calls before they reach your family." />
      </main>
      <SiteFooter />
    </WaitlistProvider>
  )
}
