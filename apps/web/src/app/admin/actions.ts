'use server'

import { headers } from 'next/headers'
import { redirect } from 'next/navigation'
import {
  adminConfigured,
  clearFailures,
  endSession,
  recordFailure,
  requireAdmin,
  startSession,
  tooManyAttempts,
  verifyPassword,
} from '@/lib/admin/auth'
import { getWaitlistStore } from '@/lib/waitlist/store'

export type LoginState = { error: string | null }

async function clientIp(): Promise<string> {
  const h = await headers()
  return h.get('x-forwarded-for')?.split(',')[0]?.trim() || h.get('x-real-ip') || 'local'
}

export async function login(_prev: LoginState, formData: FormData): Promise<LoginState> {
  if (!adminConfigured()) {
    return { error: 'Admin login isn’t configured on this server yet.' }
  }
  const ip = await clientIp()
  if (tooManyAttempts(ip)) {
    return { error: 'Too many attempts. Try again in 15 minutes.' }
  }
  const password = String(formData.get('password') ?? '')
  if (!verifyPassword(password)) {
    recordFailure(ip)
    return { error: 'That password isn’t right.' }
  }
  clearFailures(ip)
  await startSession()
  redirect('/admin')
}

export async function logout(): Promise<void> {
  await endSession()
  redirect('/admin/login')
}

export async function removeEntry(formData: FormData): Promise<void> {
  await requireAdmin()
  const id = String(formData.get('id') ?? '')
  if (/^[a-f0-9]{32}$/.test(id)) await getWaitlistStore().remove(id)
  redirect('/admin')
}
