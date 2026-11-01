import { api } from '../../services/api'

export type ShiftTemplate = { id: string; branchId: string; code: string; name: string; startTime: string; endTime: string; breakMinutes: number; status: string }
export type WorkShift = { id: string; branchId: string; employeeId: string; templateId: string | null; startAt: string; endAt: string; status: string; createdBy: string; approvedBy: string | null }
export type Availability = { id: string; employeeId: string; weekday: number; startTime: string; endTime: string }
export const schedulePermissions = (token: string, branchId: string, signal?: AbortSignal) => api<{ permissions: string[] }>(`/me?branchId=${branchId}`, token, { signal })
export const ownScheduledEmployee = (token: string, branchId: string, signal?: AbortSignal) => api<string>(`/schedules/me?branchId=${branchId}`, token, { signal })
export const templates = (token: string, branchId: string, signal?: AbortSignal) => api<ShiftTemplate[]>(`/schedules/templates?branchId=${branchId}`, token, { signal })
export const createTemplate = (token: string, branchId: string, data: { code: string; name: string; startTime: string; endTime: string; breakMinutes: number }) => api<ShiftTemplate>(`/schedules/templates?branchId=${branchId}`, token, { method: 'POST', body: JSON.stringify(data) })
export const shifts = (token: string, branchId: string, from: string, to: string, signal?: AbortSignal) => api<WorkShift[]>(`/schedules/shifts?branchId=${branchId}&from=${encodeURIComponent(from)}&to=${encodeURIComponent(to)}`, token, { signal })
export const createShift = (token: string, branchId: string, data: { employeeId: string; templateId: string | null; startAt: string; endAt: string }) => api<WorkShift>(`/schedules/shifts?branchId=${branchId}`, token, { method: 'POST', body: JSON.stringify(data) })
export const shiftAction = (token: string, branchId: string, id: string, action: 'approve' | 'cancel') => api<WorkShift>(`/schedules/shifts/${id}/${action}?branchId=${branchId}`, token, { method: 'POST' })
export const availability = (token: string, branchId: string, employeeId: string, signal?: AbortSignal) => api<Availability[]>(`/schedules/availability/${employeeId}?branchId=${branchId}`, token, { signal })
export const addAvailability = (token: string, branchId: string, employeeId: string, data: { weekday: number; startTime: string; endTime: string }) => api<Availability>(`/schedules/availability/${employeeId}?branchId=${branchId}`, token, { method: 'POST', body: JSON.stringify(data) })
