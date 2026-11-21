'use client'

import { useCallback, useEffect, useRef, useState } from 'react'

/**
 * Hero demo: a scam call to a parent's phone, handled by the assistant.
 * Steps: 0 ringing · 1–3 transcript · 4 scam flagged, family alerted.
 */
const LAST_STEP = 6
const STEP_MS = 1700

export function CallDemo() {
  const [step, setStep] = useState(4)
  const [playing, setPlaying] = useState(false)
  const timer = useRef<ReturnType<typeof setInterval> | null>(null)

  const stop = useCallback(() => {
    if (timer.current) clearInterval(timer.current)
    timer.current = null
  }, [])

  const play = useCallback(() => {
    stop()
    setStep(0)
    setPlaying(true)
    timer.current = setInterval(() => setStep((s) => (s >= LAST_STEP ? 0 : s + 1)), STEP_MS)
  }, [stop])

  const pause = useCallback(() => {
    stop()
    setStep(4)
    setPlaying(false)
  }, [stop])

  useEffect(() => {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (!reduce) play()
    return stop
  }, [play, stop])

  const talking = step >= 1 && step <= 3
  const flagged = step >= 4
  const status =
    step === 0 ? 'Ringing' : flagged ? 'Scam suspected · call ended' : 'Assistant on the call'
  const statusClass =
    step === 0
      ? 'bg-amber-bg text-amber-ink'
      : flagged
        ? 'bg-alert-bg text-alert-ink'
        : 'bg-mint text-green-deep'

  return (
    <div className="flex flex-wrap items-center justify-center gap-5">
      <figure
        aria-label="Example: the assistant answers a scam call on a parent's phone"
        className="w-[296px] flex-none rounded-[46px] bg-ink p-[11px] shadow-[0_50px_90px_-50px_rgba(17,19,18,0.5)]"
      >
        <div className="flex h-[560px] flex-col gap-4 rounded-[36px] bg-card px-[18px] pt-[30px] pb-[18px]">
          <div className="text-center">
            <p className="font-mono text-xs tracking-[0.05em] text-dim uppercase">
              Dad’s phone · Incoming
            </p>
            <p className="mt-2 font-display text-2xl font-semibold tracking-tight">
              Unknown number
            </p>
            <p className="mt-0.5 text-sm text-dim">+91 77••• ••903</p>
            <p
              className={`mt-3 inline-flex items-center gap-2 rounded-full px-3 py-1.5 text-[13px] font-semibold ${statusClass}`}
            >
              {talking && (
                <span aria-hidden="true" className="inline-flex h-3.5 items-center gap-0.5">
                  {[0, 0.15, 0.3, 0.45].map((d) => (
                    <span
                      key={d}
                      className="animate-bar h-3.5 w-[3px] rounded-sm bg-green"
                      style={{ animationDelay: `${d}s` }}
                    />
                  ))}
                </span>
              )}
              {status}
            </p>
          </div>

          <div className="flex min-h-0 flex-1 flex-col gap-2">
            {step >= 1 && (
              <Bubble who="Caller" side="right">
                Inspector Sharma, Mumbai Cyber Cell. A parcel in your name has drugs in it.
              </Bubble>
            )}
            {step >= 2 && (
              <Bubble who="Assistant" side="left">
                I’ll pass a message to him. Which station and case number can he call back on?
              </Bubble>
            )}
            {step >= 3 && (
              <Bubble who="Caller" side="right">
                No callback. Pay the fine now or he’ll be arrested.
              </Bubble>
            )}
          </div>

          {flagged ? (
            <p className="animate-rise rounded-2xl border border-alert-line bg-alert-bg px-3 py-2.5 text-[13px] leading-snug text-alert-ink">
              <b className="font-semibold">Looks like a “digital arrest” scam.</b> Nothing was
              shared. Call ended.
            </p>
          ) : (
            <div className="flex gap-2" aria-hidden="true">
              <span className="flex h-[46px] flex-1 items-center justify-center rounded-full bg-green text-[15px] font-semibold text-white">
                Take the call
              </span>
              <span className="flex h-[46px] flex-1 items-center justify-center rounded-full bg-soft text-[15px] font-semibold">
                Let it handle
              </span>
            </div>
          )}
        </div>
      </figure>

      <div className="flex min-w-0 max-w-[300px] flex-[1_1_220px] flex-col gap-2.5">
        <p className="mb-0.5 font-mono text-xs tracking-[0.05em] text-dim uppercase">Your phone</p>
        {flagged && (
          <div className="animate-rise rounded-2xl border border-alert-line bg-card px-4 py-3.5">
            <p className="text-[13px] font-semibold text-alert-ink">Alert · just now</p>
            <p className="mt-1 text-[15px] font-semibold">Scam attempt on Dad’s phone</p>
            <p className="mt-1 text-[13px] leading-snug text-body">
              Fake police caller demanded money. Dad didn’t have to speak to them. Ready to report
              to 1930.
            </p>
          </div>
        )}
        <LogItem time="11:02" title="Dr. Rao’s clinic" result="Put through to Dad" good />
        <LogItem time="09:15" title="Courier, parcel at the gate" result="Message saved" />
        <button
          type="button"
          onClick={playing ? pause : play}
          className="mt-1 min-h-11 self-start rounded-full border border-line-strong px-4 text-sm font-medium hover:bg-card"
        >
          {playing ? 'Pause the demo' : 'Replay the call'}
        </button>
      </div>
    </div>
  )
}

function Bubble({
  who,
  side,
  children,
}: {
  who: string
  side: 'left' | 'right'
  children: React.ReactNode
}) {
  const right = side === 'right'
  return (
    <p
      className={`animate-rise max-w-[92%] px-3 py-2.5 text-sm leading-snug ${
        right
          ? 'self-end rounded-[14px_14px_4px_14px] bg-ink text-white'
          : 'self-start rounded-[14px_14px_14px_4px] bg-mint text-[#06432e]'
      }`}
    >
      <b className="font-semibold">{who}</b>
      <br />
      {children}
    </p>
  )
}

function LogItem({
  time,
  title,
  result,
  good,
}: {
  time: string
  title: string
  result: string
  good?: boolean
}) {
  return (
    <div
      className={`rounded-2xl border bg-card px-4 py-3.5 ${good ? 'border-mint-line' : 'border-line'}`}
    >
      <p className="text-[13px] text-dim">{time}</p>
      <p className="mt-1 text-[15px] font-semibold">{title}</p>
      <p className={`mt-1.5 text-[13px] font-semibold ${good ? 'text-green' : 'text-dim'}`}>
        {result}
      </p>
    </div>
  )
}
