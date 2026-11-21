import type { Metadata } from 'next'
import { CallDemoSwitcher } from '@/components/call-demo'
import { JsonLd } from '@/components/json-ld'
import {
  AudienceCards,
  CallIt,
  Faq,
  FinalCta,
  Hero,
  HowItWorks,
  Privacy,
  setupSteps,
} from '@/components/sections'
import { SiteFooter, SiteHeader } from '@/components/site'
import { WaitlistProvider } from '@/components/waitlist'
import { faqs, pages } from '@/lib/content'

export const metadata: Metadata = {
  title: pages.home.title,
  description: pages.home.description,
  alternates: { canonical: pages.home.path },
  openGraph: {
    title: pages.home.shortTitle,
    description: pages.home.description,
    url: pages.home.path,
  },
}

export default function Home() {
  return (
    <WaitlistProvider>
      <SiteHeader />
      <main>
        <JsonLd page="home" faq={faqs.home} />
        <Hero
          badge="Early access on Android"
          title="Let your assistant take the call."
          body="An AI assistant that answers your phone. It handles the calls you’d rather not take, stops scammers before they reach your family, and tells you what actually matters. Even on a basic phone."
          visual={<CallDemoSwitcher />}
        />
        <AudienceCards />
        <HowItWorks title="No new number. No new phone." steps={setupSteps} />
        <CallIt
          chat={[
            { me: true, text: 'Did I miss anything important today?' },
            {
              me: false,
              text: 'Your landlord wants to visit on Saturday. A delivery was left with security, and two sales calls were declined.',
            },
            { me: true, text: 'Remind me to call him back at 6.' },
            { me: false, text: 'Done. I’ll remind you at 6 pm.' },
          ]}
        />
        <Privacy />
        <Faq items={faqs.home} />
        <FinalCta title="Let your phone ring. Your assistant has it." />
      </main>
      <SiteFooter />
    </WaitlistProvider>
  )
}
