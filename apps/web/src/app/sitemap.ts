import type { MetadataRoute } from 'next'
import { absolute, pages, UPDATED } from '@/lib/content'

export default function sitemap(): MetadataRoute.Sitemap {
  const priority: Record<keyof typeof pages, number> = {
    home: 1,
    forYou: 0.9,
    families: 0.9,
    privacy: 0.3,
  }
  return (Object.keys(pages) as (keyof typeof pages)[]).map((key) => ({
    url: absolute(pages[key].path),
    lastModified: UPDATED,
    changeFrequency: key === 'privacy' ? 'yearly' : 'monthly',
    priority: priority[key],
  }))
}
