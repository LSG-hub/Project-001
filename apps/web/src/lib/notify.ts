import 'server-only'
import type { WaitlistEntry } from './waitlist/store'

/**
 * Emails the owner when someone joins the waitlist.
 *
 * Uses any SMTP server. With Zoho Mail (already hosting the owner's domain):
 *   SMTP_HOST=smtp.zoho.in  SMTP_PORT=465  SMTP_USER=<mailbox>  SMTP_PASS=<Zoho app password>
 *   NOTIFY_TO=<where alerts go>  NOTIFY_FROM=<usually the same mailbox>
 * Locally these live in apps/web/.env.local (git-ignored); on Cloud Run, in Secret Manager.
 * Without SMTP settings, the notification is only logged.
 */
export async function notifyNewSignup(entry: WaitlistEntry, site: string): Promise<void> {
  const subject = `New waitlist sign-up #${entry.position}: ${entry.email}`
  const text = [
    `${entry.email} joined the waitlist on ${site}.`,
    '',
    `Position: ${entry.position}`,
    `Referred by: ${entry.referredBy ?? 'nobody (direct)'}`,
    `Their invite code: ${entry.referralCode}`,
    `Time: ${new Date(entry.createdAt).toLocaleString('en-IN', { timeZone: 'Asia/Kolkata' })} IST`,
    '',
    `All sign-ups: ${site}/admin`,
  ].join('\n')

  const { SMTP_HOST, SMTP_PORT, SMTP_USER, SMTP_PASS, NOTIFY_TO, NOTIFY_FROM } = process.env
  if (!SMTP_HOST || !SMTP_USER || !SMTP_PASS || !NOTIFY_TO) {
    console.info(`[notify] SMTP not configured; would have sent: ${subject}`)
    return
  }

  const nodemailer = await import('nodemailer')
  const port = Number(SMTP_PORT ?? 465)
  const transport = nodemailer.createTransport({
    host: SMTP_HOST,
    port,
    secure: port === 465,
    auth: { user: SMTP_USER, pass: SMTP_PASS },
  })

  // Never let a slow or failing mail server break the sign-up itself.
  const timeout = new Promise<never>((_, reject) =>
    setTimeout(() => reject(new Error('SMTP timeout')), 5000),
  )
  try {
    await Promise.race([
      transport.sendMail({ from: NOTIFY_FROM ?? SMTP_USER, to: NOTIFY_TO, subject, text }),
      timeout,
    ])
  } catch (err) {
    console.error('[notify] sign-up email failed', err)
  }
}
