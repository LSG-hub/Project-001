import { ImageResponse } from 'next/og'

export const size = { width: 180, height: 180 }
export const contentType = 'image/png'

export default function AppleIcon() {
  return new ImageResponse(
    <div
      style={{
        width: '100%',
        height: '100%',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: '#111312',
      }}
    >
      <svg width="150" height="150" viewBox="0 0 28 28" role="img" aria-label="Project 001">
        <path
          d="M9.5 9.5h3l1.2 3-1.6 1a6.5 6.5 0 0 0 2.9 2.9l1-1.6 3 1.2v3A1.5 1.5 0 0 1 17.5 20 9 9 0 0 1 8 11a1.5 1.5 0 0 1 1.5-1.5"
          fill="#7BE0B0"
        />
      </svg>
    </div>,
    size,
  )
}
