import type { MetadataRoute } from 'next'
import { pages, SITE_NAME } from '@/lib/content'

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: `${SITE_NAME}: an AI assistant that takes your calls`,
    short_name: SITE_NAME,
    description: pages.home.description,
    start_url: '/',
    scope: '/',
    display: 'standalone',
    background_color: '#f7f7f4',
    theme_color: '#f7f7f4',
    lang: 'en-IN',
    categories: ['utilities', 'productivity', 'security'],
    icons: [
      { src: '/icon.svg', type: 'image/svg+xml', sizes: 'any' },
      { src: '/apple-icon', type: 'image/png', sizes: '180x180' },
    ],
  }
}
