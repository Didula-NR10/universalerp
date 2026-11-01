import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { apiUrl } from '../../services/api'

export const authConfigured = import.meta.env.VITE_AUTH_MODE !== 'foundation'
const storageKey = 'erp-local-session'

export type Session = {
  access_token: string
  expiresAt: string
  user: { id: string; email: string; displayName: string; status: string }
  mustChangePassword: boolean
}
type ServerSession = Omit<Session, 'access_token'> & { accessToken: string }
type AuthContextValue = {
  session: Session | null
  loading: boolean
  signIn: (email: string, password: string) => Promise<void>
  signOut: () => Promise<void>
  changePassword: (currentPassword: string, newPassword: string) => Promise<void>
}
const AuthContext = createContext<AuthContextValue | null>(null)

function unpack(value: ServerSession): Session {
  return { access_token: value.accessToken, expiresAt: value.expiresAt, user: value.user, mustChangePassword: value.mustChangePassword }
}
async function responseError(response: Response): Promise<Error> {
  const body = await response.json().catch(() => ({})) as { detail?: string; title?: string }
  return new Error(body.detail ?? body.title ?? `Authentication failed (${response.status})`)
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(authConfigured)

  useEffect(() => {
    if (!authConfigured) { setLoading(false); return }
    const stored = window.sessionStorage.getItem(storageKey)
    if (!stored) { setLoading(false); return }
    let previous: Session
    try { previous = JSON.parse(stored) as Session } catch { window.sessionStorage.removeItem(storageKey); setLoading(false); return }
    if (!previous.access_token || Date.parse(previous.expiresAt) <= Date.now()) {
      window.sessionStorage.removeItem(storageKey); setLoading(false); return
    }
    fetch(apiUrl('/auth/session'), { headers: { Authorization: `Bearer ${previous.access_token}` } })
      .then(async response => { if (!response.ok) throw await responseError(response); return response.json() as Promise<ServerSession> })
      .then(value => { const active = unpack(value); window.sessionStorage.setItem(storageKey, JSON.stringify(active)); setSession(active) })
      .catch(() => { window.sessionStorage.removeItem(storageKey); setSession(null) })
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    const expired = () => { window.sessionStorage.removeItem(storageKey); setSession(null) }
    window.addEventListener('erp-auth-expired', expired)
    return () => window.removeEventListener('erp-auth-expired', expired)
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    session, loading,
    signIn: async (email, password) => {
      const response = await fetch(apiUrl('/auth/login'), {
        method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ email, password }),
      })
      if (!response.ok) throw await responseError(response)
      const active = unpack(await response.json() as ServerSession)
      window.sessionStorage.setItem(storageKey, JSON.stringify(active))
      setSession(active)
    },
    signOut: async () => {
      if (session) await fetch(apiUrl('/auth/logout'), { method: 'POST', headers: { Authorization: `Bearer ${session.access_token}` } }).catch(() => undefined)
      window.sessionStorage.removeItem(storageKey)
      setSession(null)
    },
    changePassword: async (currentPassword, newPassword) => {
      if (!session) throw new Error('Sign in first')
      const response = await fetch(apiUrl('/auth/change-password'), {
        method: 'POST', headers: { Authorization: `Bearer ${session.access_token}`, 'Content-Type': 'application/json' },
        body: JSON.stringify({ currentPassword, newPassword }),
      })
      if (!response.ok) throw await responseError(response)
      window.sessionStorage.removeItem(storageKey)
      setSession(null)
    },
  }), [session, loading])
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('AuthProvider is missing')
  return context
}
