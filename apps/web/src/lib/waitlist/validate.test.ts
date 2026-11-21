import { describe, expect, it } from 'vitest'
import { makeReferralCode, parseSignup } from './validate'

describe('parseSignup', () => {
  it('accepts and normalises a valid email', () => {
    expect(parseSignup({ email: '  Priya@Example.COM ' })).toEqual({
      ok: true,
      email: 'priya@example.com',
      ref: null,
    })
  })

  it('rejects malformed emails', () => {
    for (const email of ['', 'priya', 'priya@', 'priya@example', 'a b@example.com', 42]) {
      expect(parseSignup({ email })).toEqual({ ok: false, error: 'invalid_email' })
    }
  })

  it('flags bots that fill the honeypot', () => {
    expect(parseSignup({ email: 'bot@example.com', website: 'http://spam' })).toEqual({
      ok: false,
      error: 'bot',
    })
  })

  it('keeps only well-formed referral codes', () => {
    expect(parseSignup({ email: 'a@example.com', ref: 'Abc23xyz' })).toMatchObject({
      ref: 'abc23xyz',
    })
    expect(parseSignup({ email: 'a@example.com', ref: '<script>' })).toMatchObject({ ref: null })
  })
})

describe('makeReferralCode', () => {
  it('makes 8-character codes without ambiguous characters', () => {
    const code = makeReferralCode()
    expect(code).toMatch(/^[a-km-np-z2-9]{8}$/)
  })
})
