import { pages } from '@/lib/content'
import { ogImage, ogSize } from '@/lib/og'

export const alt = pages.home.shortTitle
export const size = ogSize
export const contentType = 'image/png'

export default function Image() {
  return ogImage({
    eyebrow: 'AI call assistant',
    title: pages.home.shortTitle,
    line: 'Handles the calls you’d rather not take, and stops scammers before they reach your family.',
  })
}
