import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { z } from 'zod'
import { Search, Users } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from '../organization/BranchContext'
import { contacts, createContact, updateContact, type ContactType } from './contactsApi'

const schema = z.object({ name: z.string().min(1).max(200), email: z.union([z.email(), z.literal('')]).optional(), phone: z.string().max(40).optional(), status: z.enum(['ACTIVE','INACTIVE']) })
type Values = z.infer<typeof schema>

export function ContactPage({ type }: { type: ContactType }) {
  const { session } = useAuth(); const { selectedId } = useBranches(); const client = useQueryClient()
  const token = session!.access_token; const [search,setSearch] = useState(''); const [editingId,setEditingId] = useState(''); const [message,setMessage] = useState('')
  const title = type === 'SUPPLIER' ? 'Suppliers' : 'Customers'
  const query = useQuery({ queryKey: ['contacts',type,selectedId,search], queryFn: ({ signal }) => contacts(token,type,selectedId,search,signal), enabled: Boolean(selectedId) })
  const form = useForm<Values>({ resolver: zodResolver(schema), defaultValues: { name:'',email:'',phone:'',status:'ACTIVE' } })
  const mutation = useMutation({ mutationFn: (values: Values) => editingId ? updateContact(token,type,selectedId,editingId,values) : createContact(token,type,selectedId,values), onSuccess: () => { setMessage(editingId ? 'Contact updated' : 'Contact created'); setEditingId(''); form.reset(); client.invalidateQueries({ queryKey:['contacts',type,selectedId] }) } })
  return <div className="erp-page"><div className="page-intro"><div><div className="eyebrow">CONTACTS</div><h1>{title}</h1><p>Maintain branch-specific {title.toLowerCase()} and contact details.</p></div></div>{message && <div role="status" className="form-success">{message}</div>}<div className="form-grid"><section className="work-card"><div className="work-card-header"><Users size={20}/><h2>{title}</h2></div><div className="inline-search"><Search size={16}/><input value={search} onChange={e => setSearch(e.target.value)} placeholder={`Search ${title.toLowerCase()}`} /></div>{query.isLoading && <p className="empty-copy">Loading…</p>}{query.error && <div role="alert" className="form-alert">{query.error.message}</div>}{query.data?.content.length ? <ul className="entity-list">{query.data.content.map(contact => <li key={contact.id}><div><strong>{contact.name}</strong><small>{contact.email || contact.phone || 'No contact details'}</small></div><div className="list-actions"><span className="status-pill pending">{contact.status}</span><Button variant="outline" onClick={() => { setEditingId(contact.id); form.reset({ name:contact.name,email:contact.email || '',phone:contact.phone || '',status:contact.status }) }}>Edit</Button></div></li>)}</ul> : !query.isLoading && <p className="empty-copy">No {title.toLowerCase()} found.</p>}</section><section className="work-card"><div className="work-card-header"><Users size={20}/><h2>{editingId ? `Edit ${title.slice(0,-1).toLowerCase()}` : `Add ${title.slice(0,-1).toLowerCase()}`}</h2></div><form className="work-form" onSubmit={form.handleSubmit(values => { setMessage(''); mutation.mutate(values) })}><label>Name<input {...form.register('name')} /></label><label>Email<input type="email" {...form.register('email')} /></label><label>Phone<input {...form.register('phone')} /></label>{editingId && <label>Status<select {...form.register('status')}><option value="ACTIVE">Active</option><option value="INACTIVE">Inactive</option></select></label>}{Object.keys(form.formState.errors).length > 0 && <small className="form-error">Check the contact details.</small>}{mutation.error && <div role="alert" className="form-alert">{mutation.error.message}</div>}<div className="form-actions"><Button type="submit" disabled={!selectedId || mutation.isPending}>{editingId ? 'Save changes' : 'Create contact'}</Button>{editingId && <Button variant="outline" type="button" onClick={() => { setEditingId('');form.reset() }}>Cancel</Button>}</div></form></section></div></div>
}
