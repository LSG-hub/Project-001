'use client'

import { useActionState } from 'react'
import { type LoginState, login } from '../actions'

export function LoginForm() {
  const [state, action, pending] = useActionState<LoginState, FormData>(login, { error: null })
  return (
    <form action={action} className="mt-8 flex flex-col gap-3">
      <label htmlFor="password" className="text-sm font-medium">
        Password
      </label>
      <input
        id="password"
        name="password"
        type="password"
        required
        autoComplete="current-password"
        aria-invalid={state.error ? true : undefined}
        aria-describedby={state.error ? 'login-error' : undefined}
        className="h-12 rounded-xl border border-line-strong bg-card px-4 text-base outline-green"
      />
      {state.error && (
        <p id="login-error" role="alert" className="text-sm text-[#b42318]">
          {state.error}
        </p>
      )}
      <button
        type="submit"
        disabled={pending}
        className="mt-2 h-12 rounded-xl bg-ink font-semibold text-white hover:bg-[#2a2d2c] disabled:opacity-60"
      >
        {pending ? 'Signing in…' : 'Sign in'}
      </button>
    </form>
  )
}
