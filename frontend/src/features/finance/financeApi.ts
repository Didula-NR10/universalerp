import { api } from '../../services/api'
import type { Page } from '../inventory/inventoryApi'

export type ExpenseCategory = { id: string; name: string; status: string }
export type Expense = { id: string; branchId: string; categoryId: string; description: string; amount: number; currency: string; status: 'DRAFT'|'SUBMITTED'|'APPROVED'|'REJECTED'; createdBy: string; approvedBy: string | null }
export const categories = (token: string,branchId: string,signal?:AbortSignal) => api<ExpenseCategory[]>(`/finance/categories?branchId=${branchId}`,token,{signal})
export const createCategory = (token: string,name:string) => api<ExpenseCategory>('/finance/categories',token,{method:'POST',body:JSON.stringify({name})})
export const expenses = (token: string,branchId:string,signal?:AbortSignal) => api<Page<Expense>>(`/finance/expenses?branchId=${branchId}`,token,{signal})
export const createExpense = (token:string,branchId:string,data:{categoryId:string;description:string;amount:number;currency:string}) => api<Expense>(`/finance/expenses?branchId=${branchId}`,token,{method:'POST',body:JSON.stringify(data)})
export const transitionExpense = (token:string,branchId:string,id:string,action:'submit'|'approve'|'reject') => api<Expense>(`/finance/expenses/${id}/${action}?branchId=${branchId}`,token,{method:'POST'})
export const expenseSummary = (token:string,branchId:string,currency:string,signal?:AbortSignal) => api<{currency:string;approvedTotal:number}>(`/finance/summary?branchId=${branchId}&currency=${currency}`,token,{signal})
