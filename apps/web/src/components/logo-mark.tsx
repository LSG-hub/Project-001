/**
 * The 001 logo: a green headset with a small voice wave, on a mint tile.
 * Same drawing as brand/app/logo-headset.svg and the Android launcher icon.
 * Plain SVG, so it also works inside next/og image responses.
 */
export function LogoMark({ size = 28, tile = true }: { size?: number; tile?: boolean }) {
  const green = '#0A7A53'
  return (
    <svg width={size} height={size} viewBox="0 0 108 108" aria-hidden="true">
      {tile && <rect width="108" height="108" rx="30" fill="#DDF3E6" />}
      <g transform="translate(54 54) scale(1.4) translate(-54 -59)" strokeLinecap="round" fill="none" stroke={green}>
        <path d="M36,57A18,18 0 0 1 72,57" strokeWidth="5.5" />
        <rect x="29.5" y="52" width="11.5" height="17" rx="5.5" fill={green} stroke="none" />
        <rect x="67" y="52" width="11.5" height="17" rx="5.5" fill={green} stroke="none" />
        <path d="M35.25,68C35.25,77 40,79.5 47,79.5" strokeWidth="3.6" />
        <circle cx="50.5" cy="79.5" r="3.8" fill={green} stroke="none" />
        <path d="M48,57V64M54,54V67M60,57V64" strokeWidth="3.6" />
      </g>
    </svg>
  )
}
