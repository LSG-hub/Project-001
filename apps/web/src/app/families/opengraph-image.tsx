import { pages } from '@/lib/content'
import { ogImage, ogSize } from '@/lib/og'

export const alt = pages.families.shortTitle
export const size = ogSize
export const contentType = 'image/png'

export default function Image() {
  return ogImage({
    eyebrow: 'For families',
    title: pages.families.shortTitle,
    line: 'Scam calls stop at your assistant, even on your parents’ basic phone.',
  })
}
