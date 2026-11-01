import { useState } from 'react'
import { LockKeyhole } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from './AuthProvider'

export function PasswordSettingsPage() {
  const { changePassword } = useAuth()
  const [current, setCurrent] = useState('')
  const [next, setNext] = useState('')
  const [confirm, setConfirm] = useState('')
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  return <div className="erp-page"><div className="page-intro"><div><div className="eyebrow">ACCOUNT</div><h1>Change password</h1><p>Changing your password signs out all current ERP sessions.</p></div></div><section className="work-card"><div className="work-card-header"><LockKeyhole size={20}/><h2>New password</h2></div><form className="work-form" onSubmit={async event => { event.preventDefault(); setError(''); if (next !== confirm) { setError('New passwords do not match'); return } setPending(true); try { await changePassword(current,next) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Password change failed') } finally { setPending(false) } }}><label>Current password<input required type="password" autoComplete="current-password" value={current} onChange={event => setCurrent(event.target.value)}/></label><label>New password<input required type="password" minLength={12} maxLength={128} autoComplete="new-password" value={next} onChange={event => setNext(event.target.value)}/></label><label>Confirm new password<input required type="password" minLength={12} maxLength={128} autoComplete="new-password" value={confirm} onChange={event => setConfirm(event.target.value)}/></label>{error && <div role="alert" className="form-alert">{error}</div>}<Button type="submit" disabled={pending}>{pending ? 'Saving…' : 'Change password'}</Button></form></section></div>
}
