const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]{2,}$/
const REF_RE = /^[a-z0-9]{6,12}$/

export type ParsedSignup =
  | { ok: true; email: string; ref: string | null }
  | { ok: false; error: 'invalid_email' | 'bot' }

/** Normalises and validates a waitlist form submission. */
export function parseSignup(input: {
  email: unknown
  ref?: unknown
  website?: unknown
}): ParsedSignup {
  // Honeypot: real people never see or fill the "website" field.
  if (typeof input.website === 'string' && input.website.trim() !== '') {
    return { ok: false, error: 'bot' }
  }
  if (typeof input.email !== 'string') return { ok: false, error: 'invalid_email' }
  const email = input.email.trim().toLowerCase()
  if (email.length > 254 || !EMAIL_RE.test(email)) {
    return { ok: false, error: 'invalid_email' }
  }
  const rawRef = typeof input.ref === 'string' ? input.ref.trim().toLowerCase() : ''
  const ref = REF_RE.test(rawRef) ? rawRef : null
  return { ok: true, email, ref }
}

/** Short, unambiguous referral code (no 0/o/1/l). */
export function makeReferralCode(random: () => number = Math.random): string {
  const alphabet = 'abcdefghijkmnpqrstuvwxyz23456789'
  let code = ''
  for (let i = 0; i < 8; i++) code += alphabet[Math.floor(random() * alphabet.length)]
  return code
}
