import { Suspense } from 'react'
import { Logo } from '@/components/site'
import { requireAdmin } from '@/lib/admin/auth'
import { getWaitlistStore, type ListedEntry } from '@/lib/waitlist/store'
import { logout, removeEntry } from './actions'

export default function AdminPage() {
  return (
    <main className="mx-auto max-w-[1100px] px-4 py-8 sm:px-6">
      <Suspense fallback={<p className="text-dim">Loading…</p>}>
        <AdminPanel />
      </Suspense>
    </main>
  )
}

const IST: Intl.DateTimeFormatOptions = {
  timeZone: 'Asia/Kolkata',
  day: 'numeric',
  month: 'short',
  hour: '2-digit',
  minute: '2-digit',
}

function stats(entries: ListedEntry[], now = Date.now()) {
  const day = 86_400_000
  const invites = new Map<string, number>()
  for (const e of entries) {
    if (e.referredBy) invites.set(e.referredBy, (invites.get(e.referredBy) ?? 0) + 1)
  }
  return {
    total: entries.length,
    today: entries.filter((e) => now - Date.parse(e.createdAt) < day).length,
    week: entries.filter((e) => now - Date.parse(e.createdAt) < 7 * day).length,
    referred: entries.filter((e) => e.referredBy).length,
    invites,
  }
}

async function AdminPanel() {
  await requireAdmin()
  const entries = await getWaitlistStore().list()
  const s = stats(entries)
  const newestFirst = [...entries].reverse()

  return (
    <>
      <header className="flex flex-wrap items-center justify-between gap-4">
        <Logo />
        <div className="flex items-center gap-2">
          <a
            href="/admin/export"
            className="inline-flex min-h-10 items-center rounded-full border border-line-strong bg-card px-4 text-sm font-medium hover:bg-soft"
          >
            Export CSV
          </a>
          <form action={logout}>
            <button
              type="submit"
              className="min-h-10 rounded-full bg-ink px-4 text-sm font-medium text-white hover:bg-[#2a2d2c]"
            >
              Sign out
            </button>
          </form>
        </div>
      </header>

      <h1 className="mt-10 font-display text-3xl font-semibold tracking-tight">Waitlist</h1>
      <dl className="mt-6 grid grid-cols-2 gap-3 sm:grid-cols-4">
        {[
          ['Total', s.total],
          ['Last 24 hours', s.today],
          ['Last 7 days', s.week],
          ['Came via invite', s.referred],
        ].map(([label, value]) => (
          <div key={label} className="rounded-2xl border border-line bg-card p-4">
            <dt className="text-sm text-dim">{label}</dt>
            <dd className="mt-1 font-display text-3xl font-semibold">{value}</dd>
          </div>
        ))}
      </dl>

      {entries.length === 0 ? (
        <p className="mt-10 text-body">No sign-ups yet.</p>
      ) : (
        <div className="mt-8 overflow-x-auto rounded-2xl border border-line bg-card">
          <table className="w-full min-w-[720px] text-left text-sm">
            <thead className="border-b border-line text-dim">
              <tr>
                <th className="px-4 py-3 font-medium">#</th>
                <th className="px-4 py-3 font-medium">Email</th>
                <th className="px-4 py-3 font-medium">Joined (IST)</th>
                <th className="px-4 py-3 font-medium">Invited by</th>
                <th className="px-4 py-3 font-medium">Their invites</th>
                <th className="px-4 py-3 font-medium">
                  <span className="sr-only">Actions</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {newestFirst.map((e) => (
                <tr key={e.id} className="border-b border-line last:border-0">
                  <td className="px-4 py-3 tabular-nums text-dim">{e.position}</td>
                  <td className="px-4 py-3 font-medium">{e.email}</td>
                  <td className="px-4 py-3 text-body">
                    {new Date(e.createdAt).toLocaleString('en-IN', IST)}
                  </td>
                  <td className="px-4 py-3 font-mono text-xs text-dim">{e.referredBy ?? '—'}</td>
                  <td className="px-4 py-3 tabular-nums">{s.invites.get(e.referralCode) ?? 0}</td>
                  <td className="px-4 py-3 text-right">
                    <form action={removeEntry}>
                      <input type="hidden" name="id" value={e.id} />
                      <button
                        type="submit"
                        aria-label={`Delete ${e.email}`}
                        className="min-h-9 rounded-full px-3 text-sm text-[#b42318] hover:bg-alert-bg"
                      >
                        Delete
                      </button>
                    </form>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      <p className="mt-4 text-sm text-dim">
        Delete removes the person completely, for example when they ask to be removed.
      </p>
    </>
  )
}
