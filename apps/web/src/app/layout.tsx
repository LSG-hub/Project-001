import type { Metadata, Viewport } from 'next'
import { Bricolage_Grotesque, Geist, Geist_Mono } from 'next/font/google'
import './globals.css'

const geist = Geist({ variable: '--font-geist', subsets: ['latin'] })
const geistMono = Geist_Mono({ variable: '--font-geist-mono', subsets: ['latin'], weight: '500' })
const bricolage = Bricolage_Grotesque({
  variable: '--font-bricolage',
  subsets: ['latin'],
  weight: ['600', '700'],
})

const siteUrl = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://001.sreenivasgurram.com'

export const metadata: Metadata = {
  metadataBase: new URL(siteUrl),
  title: 'Project 001: the assistant that answers scam calls for your family',
  description:
    'Unknown callers reach your assistant first, on your phone or your parents’, even a basic one. It talks to them in their language, lets real people through and alerts you when a scammer tries.',
  openGraph: {
    title: 'Project 001',
    description: 'Scammers call. Your assistant answers. Join the early-access waitlist.',
    url: '/',
    siteName: 'Project 001',
    locale: 'en_IN',
    type: 'website',
  },
  twitter: { card: 'summary_large_image' },
}

export const viewport: Viewport = {
  themeColor: '#f7f7f4',
}

export default function RootLayout({ children }: LayoutProps<'/'>) {
  return (
    <html
      lang="en"
      className={`${geist.variable} ${geistMono.variable} ${bricolage.variable} antialiased`}
    >
      <body className="min-h-screen">{children}</body>
    </html>
  )
}
