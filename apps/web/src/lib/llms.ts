import { absolute, CONTACT_EMAIL, type Faq, faqs, pages, SITE_NAME, UPDATED } from './content'

const faqBlock = (items: Faq[]) => items.map((f) => `### ${f.q}\n\n${f.a}`).join('\n\n')

/** Short Markdown index for AI agents (llmstxt.org). */
export function llmsTxt(): string {
  return `# ${SITE_NAME}

> ${pages.home.description}

${SITE_NAME} is an AI phone assistant being built in Bengaluru, India, on Google's Gemini Live speech-to-speech model. It answers calls for the user: it screens unknown numbers, handles everyday calls such as deliveries and sales pitches, stops scam calls before they reach elderly family members, and gives the user a one-line summary of each call. Users can also call the assistant to get things done. Early access is free, Android first, via a waitlist. Last updated ${UPDATED}.

## Pages

- [Home](${absolute(pages.home.path)}): what the assistant does, for individuals and families.
- [For you](${absolute(pages.forYou.path)}): for introverts, busy people and anyone who would rather not pick up.
- [For families](${absolute(pages.families.path)}): scam-call protection for parents, including on basic keypad phones.
- [Privacy](${absolute(pages.privacy.path)}): what the waitlist collects.

## More

- [Full site content as Markdown](${absolute('/llms-full.txt')})
- Contact: ${CONTACT_EMAIL}
- Source code (AGPL-3.0, with commercial licences available): https://github.com/LSG-hub/Project-001
`
}

/** The whole site's content as one Markdown file. */
export function llmsFullTxt(): string {
  return `# ${SITE_NAME}: full site content

> ${pages.home.description}

Source: ${absolute('/')} · Last updated ${UPDATED} · Status: in development, waitlist open, Android first.

## What it is

An AI assistant that answers your phone, built on Google's Gemini Live speech-to-speech model. It speaks with callers in a natural voice and in their language, mixed languages included. It always tells callers it is an AI.

It does two jobs:

1. **When someone calls you:** it answers calls you don't pick up or decline, and on smartphones every unknown number. It finds out who is calling and why, handles everyday calls itself, declines spam and scams politely, puts real people through, and sends you a one-line summary.
2. **When you call it:** you can ask what you missed, set reminders and get follow-ups done just by talking.

## For you (${absolute(pages.forYou.path)})

${pages.forYou.shortTitle} For introverts, busy people and anyone who dreads an unknown number.

- **Handles the everyday calls:** the delivery finds the gate, the sales call gets a polite no, the clinic's reminder becomes a note.
- **Your answers, ready to go:** teach it what you are always asked (gate directions, working hours, "call me after 6").
- **Type it, it says it:** type a reply during a call and the assistant says it to the caller.
- **Only what matters reaches you:** your contacts ring as usual, and you can take over any call with one tap.
- **Planned:** a separate public number to share on forms, marketplace listings and social media, so your real number stays private.

## For families (${absolute(pages.families.path)})

${pages.families.shortTitle} Scam calls stop at the assistant, even on a parent's basic keypad phone, because call forwarding is a mobile-network setting. Nothing needs to be installed on the parent's phone.

- **Live scam alerts:** fake police ("digital arrest"), bank, courier and "your son is in trouble" calls are spotted as they happen, and the family member managing the account is alerted.
- **Real people still get through:** doctors, couriers and friends are put through or leave a message.
- **What happens when a scammer calls:** the assistant never shares anything, never puts a suspected scammer through, ends the call and sends an alert. If money has already moved, it reminds you to call India's cyber-fraud helpline, 1930, straight away.

Context, with sources:
- Senior citizens in India lost ₹4,005 crore to cyber fraud in 2025 (Rajya Sabha reply): https://the420.in/india-women-senior-citizens-cyber-fraud-losses-2025/
- 770 crore fraud calls were made to Indian phones in 2025 (Truecaller): https://www.businesstoday.in/india/story/indians-received-over-4000-crore-spam-calls-in-2025-1189-crore-blocked-report-515765-2026-02-11
- 66% of unknown calls in India are spam (Truecaller): https://indiantelevision.com/mam/68-billion-spam-calls-recorded-globally-india-ranks-fifth-truecaller-report/

## How it works

1. **Switch it on in two minutes:** a one-time call-forwarding setting on Jio, Airtel, Vi or BSNL. The number stays the same.
2. **Your assistant takes the calls:** unanswered or declined calls, and on smartphones every unknown number, reach the assistant.
3. **You stay in control:** a one-line summary of every call, and an instant alert if anything looks wrong.

## Privacy rules

- Callers are always told it is an AI assistant.
- You choose what is kept: summaries, full transcripts or nothing.
- OTPs, card and account numbers are stripped out before anything is saved.
- No ads; calls are never sold or shared with advertisers.
- Data is stored in India (Google Cloud, Mumbai).

## Questions

${faqBlock(faqs.home)}

${faqBlock(faqs.forYou.filter((f) => !faqs.home.includes(f)))}

${faqBlock(faqs.families.filter((f) => !faqs.home.includes(f)))}

## Contact and licence

- Email: ${CONTACT_EMAIL}
- Source code: https://github.com/LSG-hub/Project-001 (GNU AGPL-3.0, commercial licences available)
`
}
