import { randomBytes, scryptSync } from 'node:crypto'
import { beforeAll, describe, expect, it, vi } from 'vitest'

vi.mock('server-only', () => ({}))
vi.mock('next/headers', () => ({ cookies: vi.fn() }))
vi.mock('next/navigation', () => ({ redirect: vi.fn() }))
vi.mock('next/server', () => ({ connection: vi.fn() }))

const auth = await import('./auth')

beforeAll(() => {
  const salt = randomBytes(16)
  process.env.ADMIN_PASSWORD_HASH = `scrypt:${salt.toString('hex')}:${scryptSync('correct horse battery', salt, 32).toString('hex')}`
  process.env.ADMIN_SESSION_SECRET = randomBytes(32).toString('hex')
})

describe('verifyPassword', () => {
  it('accepts the right password and rejects others', () => {
    expect(auth.verifyPassword('correct horse battery')).toBe(true)
    expect(auth.verifyPassword('correct horse batter')).toBe(false)
    expect(auth.verifyPassword('')).toBe(false)
  })
})

describe('session tokens', () => {
  it('accepts a fresh token', () => {
    const token = auth.makeSessionToken()
    expect(auth.isValidSessionToken(token ?? undefined)).toBe(true)
  })

  it('rejects expired, tampered and missing tokens', () => {
    const old = auth.makeSessionToken(Date.now() - 13 * 3600_000) ?? ''
    expect(auth.isValidSessionToken(old)).toBe(false)

    const token = auth.makeSessionToken() ?? ''
    const [exp, sig] = token.split('.')
    expect(auth.isValidSessionToken(`${Number(exp) + 3600_000}.${sig}`)).toBe(false)
    expect(auth.isValidSessionToken(`${exp}.x${sig}`)).toBe(false)
    expect(auth.isValidSessionToken(undefined)).toBe(false)
  })
})

describe('login rate limit', () => {
  it('locks an IP after 5 failures and unlocks after clearing', () => {
    const ip = '203.0.113.7'
    for (let i = 0; i < 5; i++) auth.recordFailure(ip)
    expect(auth.tooManyAttempts(ip)).toBe(true)
    auth.clearFailures(ip)
    expect(auth.tooManyAttempts(ip)).toBe(false)
  })
})
