import { absolute, CONTACT_EMAIL, type Faq, type PageKey, pages, SITE_NAME } from '@/lib/content'

const ID = {
  website: absolute('/#website'),
  org: absolute('/#organization'),
  app: absolute('/#app'),
}

const website = {
  '@type': 'WebSite',
  '@id': ID.website,
  url: absolute('/'),
  name: SITE_NAME,
  description: pages.home.description,
  inLanguage: 'en-IN',
  publisher: { '@id': ID.org },
}

const organization = {
  '@type': 'Organization',
  '@id': ID.org,
  name: SITE_NAME,
  url: absolute('/'),
  logo: absolute('/icon.svg'),
  email: CONTACT_EMAIL,
  areaServed: 'IN',
}

const app = {
  '@type': 'MobileApplication',
  '@id': ID.app,
  name: SITE_NAME,
  description: pages.home.description,
  operatingSystem: 'Android',
  applicationCategory: 'UtilitiesApplication',
  url: absolute('/'),
  publisher: { '@id': ID.org },
  offers: { '@type': 'Offer', price: '0', priceCurrency: 'INR', description: 'Free early access' },
}

/** One schema.org graph per page: site, organisation and app, plus the page and its FAQ. */
export function JsonLd({ page, faq }: { page: PageKey; faq?: Faq[] }) {
  const p = pages[page]
  const url = absolute(p.path)
  const nodes: object[] = [
    website,
    organization,
    app,
    {
      '@type': 'WebPage',
      '@id': url,
      url,
      name: p.title,
      description: p.description,
      isPartOf: { '@id': ID.website },
      about: { '@id': ID.app },
      inLanguage: 'en-IN',
      ...(page === 'home' ? {} : { breadcrumb: { '@id': `${url}#breadcrumb` } }),
    },
  ]
  if (page !== 'home') {
    nodes.push({
      '@type': 'BreadcrumbList',
      '@id': `${url}#breadcrumb`,
      itemListElement: [
        { '@type': 'ListItem', position: 1, name: SITE_NAME, item: absolute('/') },
        { '@type': 'ListItem', position: 2, name: p.shortTitle, item: url },
      ],
    })
  }
  if (faq?.length) {
    nodes.push({
      '@type': 'FAQPage',
      '@id': `${url}#faq`,
      mainEntity: faq.map((f) => ({
        '@type': 'Question',
        name: f.q,
        acceptedAnswer: { '@type': 'Answer', text: f.a },
      })),
    })
  }
  const json = JSON.stringify({ '@context': 'https://schema.org', '@graph': nodes }).replace(
    /</g,
    '\\u003c',
  )
  return <script type="application/ld+json" dangerouslySetInnerHTML={{ __html: json }} />
}
