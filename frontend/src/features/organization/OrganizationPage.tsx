import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import { Building2, Plus, Warehouse as WarehouseIcon } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from './BranchContext'
import { createBranch, createWarehouse, warehouses } from './organizationApi'
import { OrganizationDetails } from './OrganizationDetails'

const branchSchema = z.object({ code: z.string().min(1).max(32), name: z.string().min(1).max(160), timezone: z.string().max(80).optional() })
const warehouseSchema = z.object({ code: z.string().min(1).max(32), name: z.string().min(1).max(160) })

export function OrganizationPage() {
  const { session } = useAuth(); const token = session!.access_token
  const { branches, selectedId, error: branchError } = useBranches()
  const client = useQueryClient(); const [message, setMessage] = useState('')
  const branchForm = useForm<z.infer<typeof branchSchema>>({ resolver: zodResolver(branchSchema) })
  const warehouseForm = useForm<z.infer<typeof warehouseSchema>>({ resolver: zodResolver(warehouseSchema) })
  const warehouseQuery = useQuery({ queryKey: ['warehouses', selectedId], queryFn: ({ signal }) => warehouses(token, selectedId, signal), enabled: Boolean(selectedId) })
  const branchMutation = useMutation({ mutationFn: (values: z.infer<typeof branchSchema>) => createBranch(token, values), onSuccess: () => { branchForm.reset(); client.invalidateQueries({ queryKey: ['branches'] }); setMessage('Branch created') } })
  const warehouseMutation = useMutation({ mutationFn: (values: z.infer<typeof warehouseSchema>) => createWarehouse(token, selectedId, values), onSuccess: () => { warehouseForm.reset(); client.invalidateQueries({ queryKey: ['warehouses', selectedId] }); setMessage('Warehouse created') } })
  return <div className="erp-page"><div className="page-intro"><div><div className="eyebrow">ORGANIZATION</div><h1>Branches & warehouses</h1><p>Manage the locations that make up your company.</p></div></div>{message && <div role="status" className="form-success">{message}</div>}{branchError && <div role="alert" className="form-alert">{branchError}</div>}<div className="form-grid"><section className="work-card"><div className="work-card-header"><Building2 size={20}/><h2>Branches</h2></div>{branches.length ? <ul className="entity-list">{branches.map(branch => <li key={branch.id}><div><strong>{branch.name}</strong><small>{branch.code}</small></div><span className="status-pill up">{branch.status}</span></li>)}</ul> : <p className="empty-copy">No branches are visible to your account.</p>}<form onSubmit={branchForm.handleSubmit(values => { setMessage(''); branchMutation.mutate(values) })} className="work-form"><h3>Add branch</h3><label>Code<input {...branchForm.register('code')} placeholder="MAIN" /></label><label>Name<input {...branchForm.register('name')} placeholder="Main branch" /></label><label>Timezone<input {...branchForm.register('timezone')} placeholder="Asia/Colombo" /></label>{branchForm.formState.errors.code && <small className="form-error">Enter a branch code.</small>}{branchMutation.error && <div role="alert" className="form-alert">{branchMutation.error.message}</div>}<Button type="submit" disabled={branchMutation.isPending}><Plus size={15}/> Create branch</Button></form></section><section className="work-card"><div className="work-card-header"><WarehouseIcon size={20}/><h2>Warehouses</h2></div>{selectedId ? <>{warehouseQuery.isLoading && <p className="empty-copy">Loading warehouses…</p>}{warehouseQuery.error && <div role="alert" className="form-alert">{warehouseQuery.error.message}</div>}{warehouseQuery.data?.length ? <ul className="entity-list">{warehouseQuery.data.map(warehouse => <li key={warehouse.id}><div><strong>{warehouse.name}</strong><small>{warehouse.code}</small></div><span className="status-pill up">{warehouse.status}</span></li>)}</ul> : !warehouseQuery.isLoading && <p className="empty-copy">No warehouses in this branch yet.</p>}<form onSubmit={warehouseForm.handleSubmit(values => { setMessage(''); warehouseMutation.mutate(values) })} className="work-form"><h3>Add warehouse</h3><label>Code<input {...warehouseForm.register('code')} placeholder="MAIN-STOCK" /></label><label>Name<input {...warehouseForm.register('name')} placeholder="Main stockroom" /></label>{warehouseForm.formState.errors.code && <small className="form-error">Enter a warehouse code.</small>}{warehouseMutation.error && <div role="alert" className="form-alert">{warehouseMutation.error.message}</div>}<Button type="submit" disabled={warehouseMutation.isPending}><Plus size={15}/> Create warehouse</Button></form></> : <p className="empty-copy">Select a branch to manage its warehouses.</p>}</section></div><OrganizationDetails /></div>
}
