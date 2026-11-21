import 'server-only'
import { createHash } from 'node:crypto'
import { mkdir, readFile, writeFile } from 'node:fs/promises'
import path from 'node:path'
import { makeReferralCode } from './validate'

export type WaitlistEntry = {
  email: string
  referralCode: string
  referredBy: string | null
  position: number
  createdAt: string
}

export type ListedEntry = WaitlistEntry & { id: string }

export type JoinResult = { entry: WaitlistEntry; alreadyJoined: boolean }

export interface WaitlistStore {
  join(email: string, referredBy: string | null): Promise<JoinResult>
  /** All entries, oldest first. Admin only. */
  list(): Promise<ListedEntry[]>
  /** Removes one entry (for deletion requests). Positions of others don't change. */
  remove(id: string): Promise<boolean>
}

/** Document id: a hash of the email, so ids never expose addresses. */
export function idFor(email: string): string {
  return createHash('sha256').update(email).digest('hex').slice(0, 32)
}

/**
 * Firestore store, used on Cloud Run (credentials come from the service account)
 * or locally against the emulator (FIRESTORE_EMULATOR_HOST).
 */
class FirestoreWaitlistStore implements WaitlistStore {
  private async db() {
    const { Firestore, FieldValue } = await import('@google-cloud/firestore')
    // Prod uses the (default) database; the dev site sets FIRESTORE_DATABASE=development.
    const db = new Firestore({
      projectId: process.env.GOOGLE_CLOUD_PROJECT,
      databaseId: process.env.FIRESTORE_DATABASE || '(default)',
    })
    return { db, FieldValue }
  }

  async join(email: string, referredBy: string | null): Promise<JoinResult> {
    const { db, FieldValue } = await this.db()
    const ref = db.collection('waitlist').doc(idFor(email))
    const counter = db.collection('meta').doc('waitlist')

    return db.runTransaction(async (tx) => {
      const existing = await tx.get(ref)
      if (existing.exists) {
        return { entry: existing.data() as WaitlistEntry, alreadyJoined: true }
      }
      const meta = await tx.get(counter)
      const position = ((meta.data()?.count as number | undefined) ?? 0) + 1
      const entry: WaitlistEntry = {
        email,
        referralCode: makeReferralCode(),
        referredBy,
        position,
        createdAt: new Date().toISOString(),
      }
      tx.set(ref, entry)
      tx.set(counter, { count: FieldValue.increment(1) }, { merge: true })
      return { entry, alreadyJoined: false }
    })
  }

  async list(): Promise<ListedEntry[]> {
    const { db } = await this.db()
    const snap = await db.collection('waitlist').orderBy('position', 'asc').get()
    return snap.docs.map((d) => ({ id: d.id, ...(d.data() as WaitlistEntry) }))
  }

  async remove(id: string): Promise<boolean> {
    const { db } = await this.db()
    const ref = db.collection('waitlist').doc(id)
    const doc = await ref.get()
    if (!doc.exists) return false
    await ref.delete()
    return true
  }
}

/**
 * Local file store for development without any cloud setup. Writes to
 * apps/web/.data/waitlist.json, which is git-ignored.
 */
class FileWaitlistStore implements WaitlistStore {
  private file = path.join(process.cwd(), '.data', 'waitlist.json')
  private queue: Promise<unknown> = Promise.resolve()

  join(email: string, referredBy: string | null): Promise<JoinResult> {
    return this.serial(async () => {
      const entries = await this.read()
      const found = entries.find((e) => e.email === email)
      if (found) return { entry: found, alreadyJoined: true }
      const last = entries.at(-1)?.position ?? 0
      const entry: WaitlistEntry = {
        email,
        referralCode: makeReferralCode(),
        referredBy,
        position: last + 1,
        createdAt: new Date().toISOString(),
      }
      entries.push(entry)
      await this.write(entries)
      return { entry, alreadyJoined: false }
    })
  }

  async list(): Promise<ListedEntry[]> {
    const entries = await this.read()
    return entries.map((e) => ({ id: idFor(e.email), ...e }))
  }

  remove(id: string): Promise<boolean> {
    return this.serial(async () => {
      const entries = await this.read()
      const next = entries.filter((e) => idFor(e.email) !== id)
      if (next.length === entries.length) return false
      await this.write(next)
      return true
    })
  }

  private serial<T>(fn: () => Promise<T>): Promise<T> {
    const run = this.queue.then(fn)
    this.queue = run.catch(() => undefined)
    return run
  }

  private async read(): Promise<WaitlistEntry[]> {
    try {
      return JSON.parse(await readFile(this.file, 'utf8')) as WaitlistEntry[]
    } catch {
      return []
    }
  }

  private async write(entries: WaitlistEntry[]) {
    await mkdir(path.dirname(this.file), { recursive: true })
    await writeFile(this.file, JSON.stringify(entries, null, 2))
  }
}

let store: WaitlistStore | null = null

export function getWaitlistStore(): WaitlistStore {
  if (store) return store
  const useFirestore =
    process.env.WAITLIST_STORE === 'firestore' || Boolean(process.env.FIRESTORE_EMULATOR_HOST)
  store = useFirestore ? new FirestoreWaitlistStore() : new FileWaitlistStore()
  return store
}
