import type { Metadata } from 'next'

export const metadata: Metadata = {
  title: 'Admin · Project 001',
  robots: { index: false, follow: false },
}

export default function AdminLayout({ children }: LayoutProps<'/admin'>) {
  return <div className="min-h-screen bg-paper">{children}</div>
}
