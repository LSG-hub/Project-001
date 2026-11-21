import Link from 'next/link'
import type { ReactNode } from 'react'

export const CONTACT_EMAIL = 'me@sreenivasgurram.com'

export function Logo() {
  return (
    <Link
      href="/"
      className="flex items-center gap-2.5 font-display text-xl font-semibold tracking-tight"
    >
      <svg width="28" height="28" viewBox="0 0 28 28" aria-hidden="true">
        <rect width="28" height="28" rx="9" fill="#111312" />
        <path
          d="M9.5 9.5h3l1.2 3-1.6 1a6.5 6.5 0 0 0 2.9 2.9l1-1.6 3 1.2v3A1.5 1.5 0 0 1 17.5 20 9 9 0 0 1 8 11a1.5 1.5 0 0 1 1.5-1.5"
          fill="#7BE0B0"
        />
      </svg>
      Project 001
    </Link>
  )
}

export function SiteHeader({ cta = true }: { cta?: boolean }) {
  return (
    <header className="mx-auto flex max-w-[1200px] items-center justify-between gap-4 px-4 py-5 sm:px-6">
      <Logo />
      {cta && (
        <a
          href="#join"
          className="inline-flex min-h-11 items-center rounded-full bg-ink px-[18px] text-[15px] font-medium text-white hover:bg-[#2a2d2c]"
        >
          Join the waitlist
        </a>
      )}
    </header>
  )
}

export function SiteFooter() {
  return (
    <footer className="mx-auto flex max-w-[1200px] flex-wrap justify-between gap-3 px-4 pt-7 pb-10 text-sm text-dim sm:px-6">
      <span>© 2026 Project 001 · Made in Bengaluru</span>
      <span className="flex gap-5">
        <Link href="/privacy" className="hover:text-ink">
          Privacy
        </Link>
        <a href={`mailto:${CONTACT_EMAIL}`} className="hover:text-ink">
          Contact
        </a>
      </span>
    </footer>
  )
}

export function Eyebrow({
  children,
  tone = 'green',
}: {
  children: ReactNode
  tone?: 'green' | 'mint'
}) {
  return (
    <p
      className={`font-mono text-xs tracking-[0.05em] uppercase ${tone === 'green' ? 'text-green' : 'text-mint-bright'}`}
    >
      {children}
    </p>
  )
}
