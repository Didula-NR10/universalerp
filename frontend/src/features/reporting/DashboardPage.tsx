import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Area, AreaChart, CartesianGrid, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from '../organization/BranchContext'
import { dashboard, companyPermissions } from './dashboardApi'
import { Button } from '../../components/ui/button'

export function DashboardPage() {
  const token = useAuth().session!.access_token
  const { selectedId, branches, loading: branchLoading, error: branchError } = useBranches()
  const [scope, setScope] = useState<'branch' | 'company'>('branch')
  const permissions = useQuery({ queryKey: ['company-permissions'], queryFn: ({ signal }) => companyPermissions(token, signal) })
  const canViewCompany = permissions.data?.permissions.includes('REPORT_VIEW') ?? false
  const branchId = scope === 'company' && canViewCompany ? null : selectedId
  const query = useQuery({
    queryKey: ['dashboard', branchId],
    queryFn: ({ signal }) => dashboard(token, branchId, signal),
    enabled: Boolean(branchId || (scope === 'company' && canViewCompany)),
  })
  const data = query.data
  const money = (value: number) => `${data?.currency ?? ''} ${value.toFixed(2)}`
  return <div className="erp-page">
    <div className="page-intro"><div><div className="eyebrow">WORKSPACE OVERVIEW</div><h1>Operations at a glance</h1><p>Live figures for the last 30 UTC days.</p></div></div>
    <div className="report-filters"><Button variant={scope === 'branch' ? 'default' : 'outline'} onClick={() => setScope('branch')}>Selected branch</Button>{canViewCompany && <Button variant={scope === 'company' ? 'default' : 'outline'} onClick={() => setScope('company')}>All branches</Button>}<span className="empty-copy">{scope === 'branch' ? branches.find(item => item.id === selectedId)?.name ?? 'Select a branch' : 'Company-wide'}</span></div>
    {branchError && <div role="alert" className="form-alert">{branchError}</div>}
    {permissions.error && <div role="alert" className="form-alert">{permissions.error.message}</div>}
    {branchLoading || query.isLoading ? <p className="empty-copy">Loading dashboard…</p> : null}
    {query.error && <div role="alert" className="form-alert">{query.error.message}</div>}
    {!branchId && scope === 'branch' && !branchLoading && <p className="empty-copy">Select an authorized branch to view its dashboard.</p>}
    {data && <>
      <div className="report-metrics">
        <div className="readiness-card kpi-sales"><h4>Net sales</h4><div className="live-number">{money(data.salesTotal)}</div><p>{data.saleCount} transactions after approved refunds</p></div>
        <div className="readiness-card kpi-expenses"><h4>Approved expenses</h4><div className="live-number">{money(data.expenseTotal)}</div><p>Created in this period</p></div>
        <div className="readiness-card kpi-revenue"><h4>Purchase orders</h4><div className="live-number">{data.purchaseCount}</div><p>{data.pendingPurchases} awaiting approval</p></div>
      </div>
      <div className="report-metrics">
        <div className="readiness-card kpi-sales"><h4>Active employees</h4><div className="live-number">{data.employeeCount}</div><p>{data.pendingLeaves} leave requests pending</p></div>
        <div className="readiness-card kpi-warning"><h4>Low stock balances</h4><div className="live-number">{data.lowStockProducts}</div><p>Below reorder level</p></div>
        <div className="readiness-card kpi-expenses"><h4>Expenses pending</h4><div className="live-number">{data.pendingExpenses}</div><p>Awaiting decision</p></div>
      </div>
      <div className="form-grid">
        <section className="work-card"><div className="work-card-header"><h2>Daily net sales trend</h2></div>{data.revenueTrend.length ? <div className="chart-wrap" role="img" aria-label="Daily net sales chart"><ResponsiveContainer width="100%" height={260}><AreaChart data={data.revenueTrend}><CartesianGrid stroke="var(--erp-chart-grid)" vertical={false}/><XAxis dataKey="day" tick={{fontSize:10}}/><YAxis tick={{fontSize:10}}/><Tooltip/><Area type="monotone" dataKey="total" stroke="var(--erp-chart-blue)" fill="var(--erp-chart-blue)" fillOpacity={0.15}/></AreaChart></ResponsiveContainer></div> : <p className="empty-copy">No sales in this period.</p>}</section>
        <section className="work-card"><div className="work-card-header"><h2>Recent activity</h2></div>{data.recentActivity.length ? <ul className="entity-list">{data.recentActivity.map((item, index) => <li key={`${item.occurredAt}-${index}`}><div><strong>{item.action.replaceAll('_', ' ')}</strong><small>{item.resourceType} · {new Date(item.occurredAt).toLocaleString()}</small></div></li>)}</ul> : <p className="empty-copy">No recent activity.</p>}</section>
      </div>
    </>}
  </div>
}
