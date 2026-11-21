import { ImageResponse } from 'next/og'

export const ogSize = { width: 1200, height: 630 }

const phonePath =
  'M9.5 9.5h3l1.2 3-1.6 1a6.5 6.5 0 0 0 2.9 2.9l1-1.6 3 1.2v3A1.5 1.5 0 0 1 17.5 20 9 9 0 0 1 8 11a1.5 1.5 0 0 1 1.5-1.5'

/** Shared share-card design: paper background, big headline, call-green accent. */
export function ogImage({
  eyebrow,
  title,
  line,
}: {
  eyebrow: string
  title: string
  line: string
}) {
  return new ImageResponse(
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        justifyContent: 'space-between',
        background: '#f7f7f4',
        padding: '64px 72px',
        fontFamily: 'sans-serif',
        color: '#111312',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
        <svg width="56" height="56" viewBox="0 0 28 28" role="img" aria-label="Project 001 logo">
          <rect width="28" height="28" rx="9" fill="#111312" />
          <path d={phonePath} fill="#7BE0B0" />
        </svg>
        <div style={{ fontSize: 34, fontWeight: 700, letterSpacing: -0.5 }}>Project 001</div>
        <div
          style={{
            marginLeft: 'auto',
            display: 'flex',
            fontSize: 24,
            color: '#075c3f',
            background: '#ddf3e6',
            padding: '8px 18px',
            borderRadius: 999,
          }}
        >
          {eyebrow}
        </div>
      </div>
      <div style={{ display: 'flex', flexDirection: 'column', gap: 24 }}>
        <div
          style={{
            fontSize: 84,
            fontWeight: 700,
            lineHeight: 1.0,
            letterSpacing: -3,
            maxWidth: 1000,
          }}
        >
          {title}
        </div>
        <div style={{ fontSize: 32, lineHeight: 1.35, color: '#3d4240', maxWidth: 960 }}>
          {line}
        </div>
      </div>
      <div
        style={{ display: 'flex', alignItems: 'center', gap: 14, fontSize: 26, color: '#5a605d' }}
      >
        <div style={{ width: 14, height: 14, borderRadius: 999, background: '#0a7a53' }} />
        Early access on Android · Join the waitlist
      </div>
    </div>,
    ogSize,
  )
}
