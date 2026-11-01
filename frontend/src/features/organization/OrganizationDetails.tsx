import { useEffect, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from './BranchContext'
import { company, createDepartment, departments, saveCompany } from './organizationApi'

const companySchema = z.object({
  legalName: z.string().trim().min(1).max(200),
  displayName: z.string().trim().min(1).max(200),
  baseCurrency: z.string().regex(/^[A-Z]{3}$/),
  timezone: z.string().trim().min(1).max(80),
})
const departmentSchema = z.object({ code: z.string().trim().min(1).max(32), name: z.string().trim().min(1).max(160) })

export function OrganizationDetails() {
  const token = useAuth().session!.access_token
  const { selectedId } = useBranches()
  const client = useQueryClient()
  const [message, setMessage] = useState('')
  const companyQuery = useQuery({ queryKey: ['company'], queryFn: ({ signal }) => company(token, signal) })
  const departmentQuery = useQuery({ queryKey: ['departments', selectedId], queryFn: ({ signal }) => departments(token, selectedId, signal), enabled: Boolean(selectedId) })
  const companyForm = useForm<z.infer<typeof companySchema>>({ resolver: zodResolver(companySchema) })
  const departmentForm = useForm<z.infer<typeof departmentSchema>>({ resolver: zodResolver(departmentSchema) })
  useEffect(() => { if (companyQuery.data) companyForm.reset(companyQuery.data) }, [companyQuery.data, companyForm.reset])
  const companyMutation = useMutation({
    mutationFn: (values: z.infer<typeof companySchema>) => saveCompany(token, values),
    onSuccess: () => { client.invalidateQueries({ queryKey: ['company'] }); setMessage('Company profile saved') },
  })
  const departmentMutation = useMutation({
    mutationFn: (values: z.infer<typeof departmentSchema>) => createDepartment(token, selectedId, values),
    onSuccess: () => { departmentForm.reset(); client.invalidateQueries({ queryKey: ['departments', selectedId] }); setMessage('Department created') },
  })
  return <>
    {message && <div role="status" className="form-success">{message}</div>}
    <div className="form-grid">
      <section className="work-card">
        <div className="work-card-header"><h2>Company profile</h2></div>
        {companyQuery.error && <div role="alert" className="form-alert">{companyQuery.error.message}</div>}
        <form className="work-form" onSubmit={companyForm.handleSubmit(values => { setMessage(''); companyMutation.mutate(values) })}>
          <label>Legal name<input {...companyForm.register('legalName')} /></label>
          <label>Display name<input {...companyForm.register('displayName')} /></label>
          <label>Base currency<input {...companyForm.register('baseCurrency')} placeholder="USD" maxLength={3} /></label>
          <label>Timezone<input {...companyForm.register('timezone')} placeholder="Asia/Colombo" /></label>
          {Object.keys(companyForm.formState.errors).length > 0 && <small className="form-error">Enter all company details. Currency must be a three letter code.</small>}
          {companyMutation.error && <div role="alert" className="form-alert">{companyMutation.error.message}</div>}
          <Button type="submit" disabled={companyMutation.isPending}>Save company</Button>
        </form>
      </section>
      <section className="work-card">
        <div className="work-card-header"><h2>Departments</h2></div>
        {selectedId ? <>
          {departmentQuery.error && <div role="alert" className="form-alert">{departmentQuery.error.message}</div>}
          {departmentQuery.data?.length ? <ul className="entity-list">{departmentQuery.data.map(item => <li key={item.id}><div><strong>{item.name}</strong><small>{item.code}</small></div><span className="status-pill up">{item.status}</span></li>)}</ul> : <p className="empty-copy">No departments in this branch yet.</p>}
          <form className="work-form" onSubmit={departmentForm.handleSubmit(values => { setMessage(''); departmentMutation.mutate(values) })}>
            <h3>Add department</h3>
            <label>Code<input {...departmentForm.register('code')} placeholder="SALES" /></label>
            <label>Name<input {...departmentForm.register('name')} placeholder="Sales" /></label>
            {Object.keys(departmentForm.formState.errors).length > 0 && <small className="form-error">Enter a code and name.</small>}
            {departmentMutation.error && <div role="alert" className="form-alert">{departmentMutation.error.message}</div>}
            <Button type="submit" disabled={departmentMutation.isPending}>Create department</Button>
          </form>
        </> : <p className="empty-copy">Select a branch to manage its departments.</p>}
      </section>
    </div>
  </>
}
