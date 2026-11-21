'use client'

import { useCallback, useEffect, useRef, useState } from 'react'

type Line = { who: 'Caller' | 'Assistant'; text: string }
type LogItem = { time: string; title: string; result: string; tone?: 'good' | 'alert' }

type Scenario = {
  phoneLabel: string
  number: string
  lines: [Line, Line, Line]
  doneStatus: string
  doneTone: 'good' | 'alert'
  doneNote: { bold: string; rest: string }
  sideLabel: string
  sideAlert: LogItem
  log: LogItem[]
}

export const scenarios = {
  personal: {
    phoneLabel: 'Your phone · Incoming',
    number: '+91 98••• ••214',
    lines: [
      { who: 'Caller', text: 'Hi, delivery here. I’m at the gate, which tower is it?' },
      { who: 'Assistant', text: 'Hi! Tower B, flat 1204. Please leave it with security.' },
      { who: 'Caller', text: 'Okay, leaving it with the guard. Thanks!' },
    ],
    doneStatus: 'Handled for you',
    doneTone: 'good',
    doneNote: { bold: 'Delivery sorted.', rest: 'You didn’t have to pick up.' },
    sideLabel: 'Your summary',
    sideAlert: {
      time: 'Just now',
      title: 'Delivery partner',
      result: 'Left with security',
      tone: 'good',
    },
    log: [
      { time: '10:20', title: 'Landlord', result: 'Wants to visit Saturday · reply when ready' },
      { time: '09:02', title: 'Loan offer', result: 'Declined politely' },
    ],
  },
  family: {
    phoneLabel: 'Dad’s phone · Incoming',
    number: '+91 77••• ••903',
    lines: [
      {
        who: 'Caller',
        text: 'Inspector Sharma, Mumbai Cyber Cell. A parcel in your name has drugs in it.',
      },
      {
        who: 'Assistant',
        text: 'I’ll pass a message to him. Which station and case number can he call back on?',
      },
      { who: 'Caller', text: 'No callback. Pay the fine now or he’ll be arrested.' },
    ],
    doneStatus: 'Scam suspected · call ended',
    doneTone: 'alert',
    doneNote: {
      bold: 'Looks like a “digital arrest” scam.',
      rest: 'Nothing was shared. Call ended.',
    },
    sideLabel: 'Your phone',
    sideAlert: {
      time: 'Alert · just now',
      title: 'Scam attempt on Dad’s phone',
      result: 'Fake police caller demanded money. Dad didn’t have to speak to them.',
      tone: 'alert',
    },
    log: [
      { time: '11:02', title: 'Dr. Rao’s clinic', result: 'Put through to Dad', tone: 'good' },
      { time: '09:15', title: 'Courier, parcel at the gate', result: 'Message saved' },
    ],
  },
} satisfies Record<string, Scenario>

export type ScenarioKey = keyof typeof scenarios

/** Steps: 0 ringing · 1–3 transcript · 4+ outcome. */
const LAST_STEP = 6
const STEP_MS = 1700

export function CallDemo({ scenario }: { scenario: ScenarioKey }) {
  const s: Scenario = scenarios[scenario]
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
    timer.current = setInterval(() => setStep((n) => (n >= LAST_STEP ? 0 : n + 1)), STEP_MS)
  }, [stop])

  const pause = useCallback(() => {
    stop()
    setStep(4)
    setPlaying(false)
  }, [stop])

  // Restart whenever the scenario changes (and on first load), unless motion is reduced.
  useEffect(() => {
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)').matches
    if (reduce) {
      stop()
      setStep(4)
      setPlaying(false)
    } else play()
    return stop
  }, [scenario, play, stop])

  const talking = step >= 1 && step <= 3
  const done = step >= 4
  const alert = s.doneTone === 'alert'
  const status = step === 0 ? 'Ringing' : done ? s.doneStatus : 'Assistant on the call'
  const statusClass =
    step === 0
      ? 'bg-amber-bg text-amber-ink'
      : done && alert
        ? 'bg-alert-bg text-alert-ink'
        : 'bg-mint text-green-deep'

  return (
    <div className="flex flex-wrap items-center justify-center gap-5">
      <figure
        aria-label={`Example call: ${s.doneNote.bold}`}
        className="w-[296px] flex-none rounded-[46px] bg-ink p-[11px] shadow-[0_50px_90px_-50px_rgba(17,19,18,0.5)]"
      >
        <div className="flex h-[560px] flex-col gap-4 rounded-[36px] bg-card px-[18px] pt-[30px] pb-[18px]">
          <div className="text-center">
            <p className="font-mono text-xs tracking-[0.05em] text-dim uppercase">{s.phoneLabel}</p>
            <p className="mt-2 font-display text-2xl font-semibold tracking-tight">
              Unknown number
            </p>
            <p className="mt-0.5 text-sm text-dim">{s.number}</p>
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
            {s.lines.map(
              (line, i) =>
                step >= i + 1 && (
                  <Bubble key={line.text} who={line.who}>
                    {line.text}
                  </Bubble>
                ),
            )}
          </div>

          {done ? (
            <p
              className={`animate-rise rounded-2xl border px-3 py-2.5 text-[13px] leading-snug ${
                alert
                  ? 'border-alert-line bg-alert-bg text-alert-ink'
                  : 'border-mint-line bg-mint text-[#06432e]'
              }`}
            >
              <b className="font-semibold">{s.doneNote.bold}</b> {s.doneNote.rest}
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
        <p className="mb-0.5 font-mono text-xs tracking-[0.05em] text-dim uppercase">
          {s.sideLabel}
        </p>
        {done && <Log item={s.sideAlert} highlight />}
        {s.log.map((item) => (
          <Log key={item.title} item={item} />
        ))}
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

function Bubble({ who, children }: { who: Line['who']; children: React.ReactNode }) {
  const caller = who === 'Caller'
  return (
    <p
      className={`animate-rise max-w-[92%] px-3 py-2.5 text-sm leading-snug ${
        caller
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

function Log({ item, highlight }: { item: LogItem; highlight?: boolean }) {
  const border =
    item.tone === 'alert'
      ? 'border-alert-line'
      : item.tone === 'good'
        ? 'border-mint-line'
        : 'border-line'
  const resultColor =
    item.tone === 'alert' ? 'text-body' : item.tone === 'good' ? 'text-green' : 'text-dim'
  return (
    <div
      className={`rounded-2xl border bg-card px-4 py-3.5 ${border} ${highlight ? 'animate-rise' : ''}`}
    >
      <p
        className={`text-[13px] ${item.tone === 'alert' ? 'font-semibold text-alert-ink' : 'text-dim'}`}
      >
        {item.time}
      </p>
      <p className="mt-1 text-[15px] font-semibold">{item.title}</p>
      <p
        className={`mt-1.5 text-[13px] leading-snug ${item.tone === 'alert' ? '' : 'font-semibold'} ${resultColor}`}
      >
        {item.result}
      </p>
    </div>
  )
}

/** Home hero: lets visitors switch between the two stories. */
export function CallDemoSwitcher() {
  const [scenario, setScenario] = useState<ScenarioKey>('personal')
  const tabs: { key: ScenarioKey; label: string }[] = [
    { key: 'personal', label: 'For you' },
    { key: 'family', label: 'For your parents' },
  ]
  return (
    <div className="flex flex-col items-center gap-5">
      <fieldset className="inline-flex rounded-full border border-line-strong bg-card p-1">
        <legend className="sr-only">Choose an example call</legend>
        {tabs.map((t) => (
          <label
            key={t.key}
            className={`relative cursor-pointer rounded-full px-4 py-2 text-sm font-medium transition-colors has-[:focus-visible]:outline-2 has-[:focus-visible]:outline-green ${
              scenario === t.key ? 'bg-ink text-white' : 'text-body hover:text-ink'
            }`}
          >
            <input
              type="radio"
              name="demo-scenario"
              value={t.key}
              checked={scenario === t.key}
              onChange={() => setScenario(t.key)}
              className="sr-only"
            />
            {t.label}
          </label>
        ))}
      </fieldset>
      <CallDemo scenario={scenario} />
    </div>
  )
}
