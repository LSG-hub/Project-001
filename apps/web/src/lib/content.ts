/**
 * Single source for page copy that is also used by structured data,
 * the sitemap and llms.txt, so they never drift apart.
 */

export const SITE_URL = process.env.NEXT_PUBLIC_SITE_URL ?? 'https://001.sreenivasgurram.com'
export const SITE_NAME = 'Project 001'
export const CONTACT_EMAIL = 'me@sreenivasgurram.com'
export const UPDATED = '2026-11-21'

export type Faq = { q: string; a: string }

export const pages = {
  home: {
    path: '/',
    title: 'Project 001: an AI assistant that takes your calls',
    shortTitle: 'Let your assistant take the call.',
    description:
      'An AI assistant that answers your phone. It handles the calls you’d rather not take, stops scammers before they reach your family, and tells you what matters. Even on a basic phone.',
  },
  forYou: {
    path: '/for-you',
    title: 'For you · Project 001',
    shortTitle: 'Never pick up a call you don’t want to.',
    description:
      'For introverts, busy people and anyone who’d rather not pick up. Your AI assistant answers, handles the call or sums it up, and you reply when you’re ready, by text if you like.',
  },
  families: {
    path: '/families',
    title: 'For families · Project 001',
    shortTitle: 'Scammers call. Your assistant answers.',
    description:
      'Scam calls stop at your assistant, even on your parents’ basic phone. It talks to callers in their language, lets real people through and alerts you the moment a scammer tries.',
  },
  privacy: {
    path: '/privacy',
    title: 'Privacy · Project 001',
    shortTitle: 'Privacy',
    description: 'What we collect on the Project 001 waitlist and how we use it.',
  },
} as const

export type PageKey = keyof typeof pages

const faq = {
  number: {
    q: 'Do I need a new phone number?',
    a: 'No. You keep your number. Calls reach the assistant through a one-time forwarding setting, only when you don’t pick up, decline, or (on a smartphone) the number is unknown.',
  },
  ai: {
    q: 'Will callers know it’s an AI?',
    a: 'Yes. It always says so at the start of the call. Being honest with callers is one of our rules.',
  },
  languages: {
    q: 'Which languages does it speak?',
    a: 'It’s being built for the way India talks, starting with Hindi, English and Telugu, mixed languages included. More Indian languages will follow.',
  },
  cost: {
    q: 'What will it cost?',
    a: 'Early access is free. Later there will be simple plans, with extra minutes you add only if you need them. Spam and scam calls never count against your minutes.',
  },
  when: {
    q: 'When can I try it?',
    a: 'Invites go out in small batches, Android first. Join the waitlist and we’ll email you when it’s your turn.',
  },
  takeCalls: {
    q: 'Can I still take calls myself?',
    a: 'Always. Your saved contacts ring as normal, and you can take over any call the assistant is handling with one tap.',
  },
  smartphone: {
    q: 'Do my parents need a smartphone?',
    a: 'No. Call forwarding is a setting on the mobile network, so it works on any phone, including basic keypad phones. You manage everything from your own app.',
  },
  scammer: {
    q: 'What happens when a scammer calls?',
    a: 'The assistant never shares anything and never puts a suspected scammer through. It ends the call, and you get an alert with what was said. If money has already moved, it reminds you to call 1930 straight away.',
  },
} satisfies Record<string, Faq>

export const faqs: Record<'home' | 'forYou' | 'families', Faq[]> = {
  home: [faq.number, faq.ai, faq.languages, faq.cost, faq.when],
  forYou: [faq.ai, faq.takeCalls, faq.number, faq.languages, faq.cost],
  families: [faq.smartphone, faq.scammer, faq.number, faq.languages, faq.cost],
}

export const absolute = (path: string) => new URL(path, SITE_URL).toString()
