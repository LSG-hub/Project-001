import { Logo } from '@/components/site'
import { LoginForm } from './login-form'

export default function AdminLoginPage() {
  return (
    <main className="mx-auto flex min-h-screen max-w-[400px] flex-col justify-center px-4">
      <Logo />
      <h1 className="mt-8 font-display text-3xl font-semibold tracking-tight">Admin sign-in</h1>
      <p className="mt-2 text-[15px] text-body">For the site owner only.</p>
      <LoginForm />
    </main>
  )
}
