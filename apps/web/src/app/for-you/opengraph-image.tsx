import { pages } from '@/lib/content'
import { ogImage, ogSize } from '@/lib/og'

export const alt = pages.forYou.shortTitle
export const size = ogSize
export const contentType = 'image/png'

export default function Image() {
  return ogImage({
    eyebrow: 'For you',
    title: pages.forYou.shortTitle,
    line: 'Your assistant answers, sorts it out or sums it up. You reply when you’re ready.',
  })
}
