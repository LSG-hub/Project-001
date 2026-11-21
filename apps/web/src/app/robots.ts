import type { MetadataRoute } from 'next'
import { absolute } from '@/lib/content'

// Search engines and AI crawlers (GPTBot, ClaudeBot, PerplexityBot, Google-Extended…)
// are all welcome; they fall under the one "*" group. A Markdown summary for AI
// agents lives at /llms.txt. The dev site is kept out of search by an
// X-Robots-Tag: noindex header set in firebase.json.
export default function robots(): MetadataRoute.Robots {
  return {
    rules: [{ userAgent: '*', allow: '/', disallow: ['/admin'] }],
    sitemap: absolute('/sitemap.xml'),
  }
}
