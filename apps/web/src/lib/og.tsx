import { ImageResponse } from 'next/og'
import { LogoMark } from '@/components/logo-mark'

export const ogSize = { width: 1200, height: 630 }

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
        <LogoMark size={56} />
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
