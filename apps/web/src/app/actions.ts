'use server'

import { notifyNewSignup } from '@/lib/notify'
import { getWaitlistStore } from '@/lib/waitlist/store'
import { parseSignup } from '@/lib/waitlist/validate'

export type WaitlistState =
  | { status: 'idle' }
  | { status: 'error'; message: string; email: string }
  | {
      status: 'joined'
      email: string
      position: number
      referralCode: string
      alreadyJoined: boolean
    }

export async function joinWaitlist(
  _prev: WaitlistState,
  formData: FormData,
): Promise<WaitlistState> {
  const parsed = parseSignup({
    email: formData.get('email'),
    ref: formData.get('ref'),
    website: formData.get('website'),
  })
  const typed = String(formData.get('email') ?? '')

  if (!parsed.ok) {
    if (parsed.error === 'bot') {
      // Look successful to bots, store nothing.
      return { status: 'joined', email: typed, position: 0, referralCode: '', alreadyJoined: false }
    }
    return {
      status: 'error',
      message: "That email doesn't look complete. Mind checking it?",
      email: typed,
    }
  }

  try {
    const { entry, alreadyJoined } = await getWaitlistStore().join(parsed.email, parsed.ref)
    if (!alreadyJoined) {
      // SITE_URL is set per Cloud Run service (dev or prod), read at request time.
      const site = process.env.SITE_URL ?? 'https://001.sreenivasgurram.com'
      await notifyNewSignup(entry, site)
    }
    return {
      status: 'joined',
      email: entry.email,
      position: entry.position,
      referralCode: entry.referralCode,
      alreadyJoined,
    }
  } catch (err) {
    console.error('waitlist join failed', err)
    return {
      status: 'error',
      message: 'Something went wrong on our side. Please try again.',
      email: typed,
    }
  }
}
