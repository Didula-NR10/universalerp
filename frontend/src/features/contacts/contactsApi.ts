import { api } from '../../services/api'
import type { Page } from '../inventory/inventoryApi'

export type ContactType = 'SUPPLIER' | 'CUSTOMER'
export type Contact = { id: string; branchId: string; type: ContactType; name: string; email: string | null; phone: string | null; status: 'ACTIVE' | 'INACTIVE' }
type ContactInput = { name: string; email?: string; phone?: string; status?: string }
export const contacts = (token: string, type: ContactType, branchId: string, search: string, signal?: AbortSignal) => api<Page<Contact>>(`/contacts/${type}?branchId=${branchId}&search=${encodeURIComponent(search)}`, token, { signal })
export const createContact = (token: string, type: ContactType, branchId: string, input: ContactInput) => api<Contact>(`/contacts/${type}?branchId=${branchId}`, token, { method: 'POST', body: JSON.stringify(input) })
export const updateContact = (token: string, type: ContactType, branchId: string, id: string, input: ContactInput) => api<Contact>(`/contacts/${type}/${id}?branchId=${branchId}`, token, { method: 'PUT', body: JSON.stringify(input) })
