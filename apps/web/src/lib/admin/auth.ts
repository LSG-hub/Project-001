import 'server-only'
import { createHmac, scryptSync, timingSafeEqual } from 'node:crypto'
import { cookies } from 'next/headers'
import { redirect } from 'next/navigation'
import { connection } from 'next/server'

/**
 * Single-owner admin login: a password checked against a scrypt hash, then a
 * signed, HTTP-only session cookie. Configure with:
 *   ADMIN_PASSWORD_HASH   "scrypt:<salt hex>:<hash hex>"  (make one: npm run admin:hash -w @project001/web)
 *   ADMIN_SESSION_SECRET  32+ random bytes, hex
 * Locally: apps/web/.env.local (git-ignored). On Cloud Run: Secret Manager.
 */

const COOKIE = 'p001_admin'
const SESSION_HOURS = 12

function secret(): Buffer | null {
  const s = process.env.ADMIN_SESSION_SECRET
  return s && s.length >= 32 ? Buffer.from(s) : null
}

export function adminConfigured(): boolean {
  return Boolean(process.env.ADMIN_PASSWORD_HASH && secret())
}

export function verifyPassword(password: string): boolean {
  const stored = process.env.ADMIN_PASSWORD_HASH ?? ''
  const [scheme, saltHex, hashHex] = stored.split(':')
  if (scheme !== 'scrypt' || !saltHex || !hashHex) return false
  const expected = Buffer.from(hashHex, 'hex')
  const actual = scryptSync(password, Buffer.from(saltHex, 'hex'), expected.length)
  return expected.length > 0 && timingSafeEqual(actual, expected)
}

function sign(payload: string, key: Buffer): string {
  return createHmac('sha256', key).update(payload).digest('base64url')
}

/** Creates the session token: "<expiry ms>.<signature>". */
export function makeSessionToken(now = Date.now()): string | null {
  const key = secret()
  if (!key) return null
  const exp = String(now + SESSION_HOURS * 3600_000)
  return `${exp}.${sign(exp, key)}`
}

export function isValidSessionToken(token: string | undefined, now = Date.now()): boolean {
  const key = secret()
  if (!key || !token) return false
  const [exp, sig] = token.split('.')
  if (!exp || !sig || !/^\d+$/.test(exp) || Number(exp) < now) return false
  const expected = Buffer.from(sign(exp, key))
  const given = Buffer.from(sig)
  return expected.length === given.length && timingSafeEqual(expected, given)
}

export async function startSession(): Promise<boolean> {
  const token = makeSessionToken()
  if (!token) return false
  ;(await cookies()).set(COOKIE, token, {
    httpOnly: true,
    secure: process.env.NODE_ENV === 'production',
    sameSite: 'strict',
    path: '/',
    maxAge: SESSION_HOURS * 3600,
  })
  return true
}

export async function endSession(): Promise<void> {
  ;(await cookies()).delete(COOKIE)
}

export async function isAdmin(): Promise<boolean> {
  // Session checks depend on the request and the current time, so never prerender them.
  await connection()
  return isValidSessionToken((await cookies()).get(COOKIE)?.value)
}

/** Call at the top of every admin page, action and route. */
export async function requireAdmin(): Promise<void> {
  if (!(await isAdmin())) redirect('/admin/login')
}

// Basic brute-force protection: 5 failed attempts per IP per 15 minutes (per instance).
const attempts = new Map<string, { count: number; until: number }>()

export function tooManyAttempts(ip: string, now = Date.now()): boolean {
  const a = attempts.get(ip)
  return Boolean(a && a.until > now && a.count >= 5)
}

export function recordFailure(ip: string, now = Date.now()): void {
  const a = attempts.get(ip)
  if (!a || a.until < now) attempts.set(ip, { count: 1, until: now + 15 * 60_000 })
  else a.count += 1
}

export function clearFailures(ip: string): void {
  attempts.delete(ip)
}
