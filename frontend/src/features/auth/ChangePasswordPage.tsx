import { useState } from 'react'
import { LockKeyhole } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from './AuthProvider'

export function ChangePasswordPage() {
  const { changePassword } = useAuth()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  return <main className="login-page"><div className="login-card"><div className="login-logo"><LockKeyhole size={25}/></div><h1>Set a new password</h1><p>Choose a private password before using the ERP. Use at least 12 characters.</p><form onSubmit={async event => { event.preventDefault(); setError(''); if (next !== confirm) { setError('New passwords do not match'); return } setPending(true); try { await changePassword(current, next) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Password change failed') } finally { setPending(false) } }}><label>Current password<input required type="password" autoComplete="current-password" value={current} onChange={event => setCurrent(event.target.value)} /></label><label>New password<input required type="password" minLength={12} maxLength={128} autoComplete="new-password" value={next} onChange={event => setNext(event.target.value)} /></label><label>Confirm new password<input required type="password" minLength={12} maxLength={128} autoComplete="new-password" value={confirm} onChange={event => setConfirm(event.target.value)} /></label>{error && <div role="alert" className="form-alert">{error}</div>}<Button type="submit" disabled={pending}>{pending ? 'Saving…' : 'Change password'}</Button></form></div></main>
}
