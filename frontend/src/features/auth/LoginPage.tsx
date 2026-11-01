import { useState } from 'react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Command, LockKeyhole } from 'lucide-react'
import { useAuth } from './AuthProvider'
import { Button } from '../../components/ui/button'

const schema = z.object({ email: z.email(), password: z.string().min(1) })
type Values = z.infer<typeof schema>

export function LoginPage() {
  const { signIn } = useAuth()
  const [error, setError] = useState('')
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<Values>({ resolver: zodResolver(schema) })
  return <main className="login-page"><div className="login-card"><div className="login-logo"><Command size={25}/></div><div className="eyebrow">UNIVERSAL ERP</div><h1>Sign in to your workspace</h1><p>Use your ERP email and password.</p><form onSubmit={handleSubmit(async values => { setError(''); try { await signIn(values.email, values.password) } catch (cause) { setError(cause instanceof Error ? cause.message : 'Sign in failed') } })}><label>Email address<input autoComplete="email" type="email" {...register('email')}/></label>{errors.email && <small className="form-error">{errors.email.message}</small>}<label>Password<input autoComplete="current-password" type="password" {...register('password')}/></label>{errors.password && <small className="form-error">{errors.password.message}</small>}{error && <div role="alert" className="form-alert">{error}</div>}<Button type="submit" disabled={isSubmitting} className="login-submit">{isSubmitting ? 'Signing in…' : 'Sign in'}</Button></form><div className="login-note"><LockKeyhole size={14}/> Protected by ERP authentication</div></div></main>
}
