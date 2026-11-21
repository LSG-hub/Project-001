import type { Metadata, Viewport } from 'next'
import { Bricolage_Grotesque, Geist, Geist_Mono } from 'next/font/google'
import { SITE_NAME, SITE_URL } from '@/lib/content'
import './globals.css'

const geist = Geist({ variable: '--font-geist', subsets: ['latin'] })
const geistMono = Geist_Mono({ variable: '--font-geist-mono', subsets: ['latin'], weight: '500' })
const bricolage = Bricolage_Grotesque({
  variable: '--font-bricolage',
  subsets: ['latin'],
  weight: ['600', '700'],
})

export const metadata: Metadata = {
  metadataBase: new URL(SITE_URL),
  applicationName: SITE_NAME,
  alternates: { canonical: '/' },
  title: 'Project 001: an AI assistant that takes your calls',
  description:
    'An AI assistant that answers your phone. It handles the calls you’d rather not take, stops scammers before they reach your family, and tells you what matters. Even on a basic phone.',
  openGraph: {
    title: 'Project 001',
    description: 'Let your assistant take the call. Join the early-access waitlist.',
    url: '/',
    siteName: 'Project 001',
    locale: 'en_IN',
    type: 'website',
  },
  twitter: { card: 'summary_large_image' },
  formatDetection: { telephone: false },
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
