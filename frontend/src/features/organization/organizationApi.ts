import { api } from '../../services/api'

export type Branch = { id: string; code: string; name: string; status: string; timezone: string | null }
export type Warehouse = { id: string; branchId: string; code: string; name: string; status: string }
export type Department = { id: string; branchId: string; code: string; name: string; status: string }
export type Company = { legalName: string; displayName: string; baseCurrency: string; timezone: string }
export const company = (token: string, signal?: AbortSignal) => api<Company>('/company', token, { signal })
export const saveCompany = (token: string, data: Company) => api<Company>('/company', token, { method: 'PUT', body: JSON.stringify(data) })
export const branches = (token: string, signal?: AbortSignal) => api<Branch[]>('/branches', token, { signal })
export const warehouses = (token: string, branchId: string, signal?: AbortSignal) => api<Warehouse[]>(`/branches/${branchId}/warehouses`, token, { signal })
export const departments = (token: string, branchId: string, signal?: AbortSignal) => api<Department[]>(`/branches/${branchId}/departments`, token, { signal })
export const createBranch = (token: string, data: { code: string; name: string; timezone?: string }) => api<Branch>('/branches', token, { method: 'POST', body: JSON.stringify(data) })
export const createWarehouse = (token: string, branchId: string, data: { code: string; name: string }) => api<Warehouse>(`/branches/${branchId}/warehouses`, token, { method: 'POST', body: JSON.stringify(data) })
export const createDepartment = (token: string, branchId: string, data: { code: string; name: string }) => api<Department>(`/branches/${branchId}/departments`, token, { method: 'POST', body: JSON.stringify(data) })
