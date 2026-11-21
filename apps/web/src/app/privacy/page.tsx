import type { Metadata } from 'next'
import { JsonLd } from '@/components/json-ld'
import { CONTACT_EMAIL, SiteFooter, SiteHeader } from '@/components/site'
import { pages } from '@/lib/content'

export const metadata: Metadata = {
  title: pages.privacy.title,
  description: pages.privacy.description,
  alternates: { canonical: pages.privacy.path },
  openGraph: {
    title: pages.privacy.shortTitle,
    description: pages.privacy.description,
    url: pages.privacy.path,
  },
}

export default function PrivacyPage() {
  return (
    <>
      <SiteHeader cta={false} />
      <main className="mx-auto max-w-[720px] px-4 pt-10 pb-20 sm:px-6">
        <JsonLd page="privacy" />
        <h1 className="font-display text-4xl font-semibold tracking-[-0.03em] sm:text-5xl">
          Privacy
        </h1>
        <p className="mt-3 text-sm text-dim">Last updated 21 November 2026</p>
        <div className="mt-10 space-y-8 text-base leading-relaxed text-body">
          <Block title="What this page covers">
            This notice covers the waitlist on this website. The assistant itself isn’t available
            yet. It will come with its own, more detailed privacy policy before anyone uses it.
          </Block>
          <Block title="What we collect">
            Your email address, the date you joined, your place in the queue, and the referral code
            of whoever invited you, if anyone did. Nothing else. We don’t use advertising or
            tracking cookies on this site.
          </Block>
          <Block title="How we use it">
            Only to email you about your invite and major updates on early access. We don’t sell,
            rent or share your email with anyone.
          </Block>
          <Block title="Where it’s stored">
            On Google Cloud in Mumbai, India (region asia-south1).
          </Block>
          <Block title="Your choices">
            Want to be removed? Email{' '}
            <a className="font-medium text-green underline" href={`mailto:${CONTACT_EMAIL}`}>
              {CONTACT_EMAIL}
            </a>{' '}
            and we’ll delete your details within 7 days.
          </Block>
        </div>
      </main>
      <SiteFooter />
    </>
  )
}

function Block({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <section>
      <h2 className="text-xl font-semibold text-ink">{title}</h2>
      <p className="mt-2">{children}</p>
    </section>
  )
}
