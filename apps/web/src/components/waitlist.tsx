'use client'

import {
  createContext,
  type ReactNode,
  useActionState,
  useContext,
  useEffect,
  useId,
  useState,
} from 'react'
import { joinWaitlist, type WaitlistState } from '@/app/actions'

type Ctx = {
  state: WaitlistState
  formAction: (payload: FormData) => void
  pending: boolean
  referredBy: string
}

const WaitlistContext = createContext<Ctx | null>(null)

/** One waitlist state shared by every form on the page. */
export function WaitlistProvider({ children }: { children: ReactNode }) {
  const [state, formAction, pending] = useActionState(joinWaitlist, { status: 'idle' })
  const [referredBy, setReferredBy] = useState('')

  useEffect(() => {
    const ref = new URLSearchParams(window.location.search).get('ref')
    if (ref) setReferredBy(ref)
  }, [])

  return (
    <WaitlistContext value={{ state, formAction, pending, referredBy }}>{children}</WaitlistContext>
  )
}

function useWaitlist(): Ctx {
  const ctx = useContext(WaitlistContext)
  if (!ctx) throw new Error('WaitlistForm must be inside WaitlistProvider')
  return ctx
}

export function WaitlistForm({
  tone = 'light',
  compact = false,
}: {
  tone?: 'light' | 'dark'
  compact?: boolean
}) {
  const { state, formAction, pending, referredBy } = useWaitlist()
  const inputId = useId()
  const errorId = useId()

  if (state.status === 'joined') {
    return compact ? (
      <p className="text-lg font-semibold text-mint-bright">
        You’re on the list. We’ll be in touch.
      </p>
    ) : (
      <JoinedCard state={state} />
    )
  }

  const dark = tone === 'dark'
  const error = state.status === 'error' ? state.message : null

  return (
    <div>
      <form
        action={formAction}
        className="flex flex-wrap gap-2 rounded-[28px] border border-line-strong bg-card p-1.5 sm:rounded-full"
      >
        <label htmlFor={inputId} className="sr-only">
          Email address
        </label>
        <input
          id={inputId}
          name="email"
          type="email"
          required
          autoComplete="email"
          inputMode="email"
          placeholder="you@example.com"
          defaultValue={state.status === 'error' ? state.email : ''}
          aria-invalid={error ? true : undefined}
          aria-describedby={error ? errorId : undefined}
          className="h-13 min-w-0 flex-[1_1_220px] rounded-full bg-transparent px-5 text-[17px] text-ink outline-green placeholder:text-[#7c8280]"
        />
        <input type="hidden" name="ref" value={referredBy} />
        {/* Honeypot for bots; hidden from people and assistive tech. */}
        <div aria-hidden="true" className="absolute -left-[9999px] h-px w-px overflow-hidden">
          <label>
            Website
            <input type="text" name="website" tabIndex={-1} autoComplete="off" />
          </label>
        </div>
        <button
          type="submit"
          disabled={pending}
          className={`h-13 flex-[1_0_auto] rounded-full px-6 text-base font-semibold text-white transition-colors disabled:opacity-60 sm:flex-none ${
            dark ? 'bg-green hover:bg-green-deep' : 'bg-ink hover:bg-[#2a2d2c]'
          }`}
        >
          {pending ? 'Joining…' : 'Join the waitlist'}
        </button>
      </form>
      {error ? (
        <p
          id={errorId}
          role="alert"
          className={`mt-3 ml-5 text-sm ${dark ? 'text-[#ffb4ab]' : 'text-[#b42318]'}`}
        >
          {error}
        </p>
      ) : (
        !compact && (
          <p className="mt-3.5 ml-5 text-sm text-dim">
            Email only. One message when your invite is ready.
          </p>
        )
      )}
    </div>
  )
}

function JoinedCard({ state }: { state: Extract<WaitlistState, { status: 'joined' }> }) {
  const [copied, setCopied] = useState(false)
  const [origin, setOrigin] = useState('')
  useEffect(() => setOrigin(window.location.origin), [])

  const link = state.referralCode ? `${origin}/?ref=${state.referralCode}` : origin
  const shareText = `I joined the waitlist for an AI assistant that answers scam calls for you and your family. Join here: ${link}`

  async function copy() {
    try {
      await navigator.clipboard.writeText(link)
      setCopied(true)
    } catch {
      setCopied(false)
    }
  }

  return (
    <div className="animate-rise rounded-3xl border border-mint-line bg-card p-6" role="status">
      <p className="font-display text-2xl font-semibold tracking-tight">
        {state.alreadyJoined ? 'You’re already on the list.' : 'You’re on the list.'}
      </p>
      <p className="mt-1.5 text-[15px] leading-relaxed text-body">
        {state.position > 0 ? <>You’re number {state.position}. </> : null}
        We’ll email <span className="font-medium text-ink">{state.email}</span> when it’s your turn.
        Know someone whose parents get these calls? Send them the link.
      </p>
      <div className="mt-4 flex flex-wrap gap-2">
        <a
          href={`https://wa.me/?text=${encodeURIComponent(shareText)}`}
          target="_blank"
          rel="noopener noreferrer"
          className="inline-flex min-h-11 items-center rounded-full bg-green px-5 text-[15px] font-semibold text-white hover:bg-green-deep"
        >
          Share on WhatsApp
        </a>
        <button
          type="button"
          onClick={copy}
          className="min-h-11 rounded-full border border-line-strong bg-card px-5 text-[15px] font-medium hover:bg-soft"
        >
          {copied ? 'Link copied' : 'Copy invite link'}
        </button>
      </div>
    </div>
  )
}
