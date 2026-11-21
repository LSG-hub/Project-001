import { isAdmin } from '@/lib/admin/auth'
import { getWaitlistStore } from '@/lib/waitlist/store'

function csvCell(value: string | number | null): string {
  const s = String(value ?? '')
  // Quote everything and neutralise spreadsheet formula injection.
  const safe = /^[=+\-@]/.test(s) ? `'${s}` : s
  return `"${safe.replaceAll('"', '""')}"`
}

export async function GET(request: Request) {
  if (!(await isAdmin())) {
    return Response.redirect(new URL('/admin/login', request.url), 303)
  }
  const entries = await getWaitlistStore().list()
  const rows = [
    ['position', 'email', 'joined_at', 'referred_by', 'referral_code'],
    ...entries.map((e) => [e.position, e.email, e.createdAt, e.referredBy, e.referralCode]),
  ]
  const csv = `${rows.map((r) => r.map(csvCell).join(',')).join('\n')}\n`
  return new Response(csv, {
    headers: {
      'Content-Type': 'text/csv; charset=utf-8',
      'Content-Disposition': `attachment; filename="waitlist-${new Date().toISOString().slice(0, 10)}.csv"`,
      'Cache-Control': 'no-store',
    },
  })
}
