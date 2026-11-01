import { api } from '../../services/api'

export type Dashboard = {
  currency: string
  branchId: string | null
  from: string
  to: string
  salesTotal: number
  expenseTotal: number
  saleCount: number
  purchaseCount: number
  employeeCount: number
  lowStockProducts: number
  pendingPurchases: number
  pendingExpenses: number
  pendingLeaves: number
  revenueTrend: { day: string; total: number }[]
  recentActivity: { action: string; resourceType: string; occurredAt: string }[]
}

export const dashboard = (token: string, branchId: string | null, signal?: AbortSignal) =>
  api<Dashboard>(`/dashboard${branchId ? `?branchId=${encodeURIComponent(branchId)}` : ''}`, token, { signal })

export const companyPermissions = (token: string, signal?: AbortSignal) =>
  api<{ permissions: string[] }>('/me', token, { signal })
