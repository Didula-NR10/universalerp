import { api } from '../../services/api'

export type Product = { id: string; sku: string; barcode: string | null; name: string; price: number; currency: string; reorderLevel: number; taxRate: number; status: string }
export type Page<T> = { content: T[]; totalElements: number; totalPages: number; number: number }
export type StockMovement = { id: string; warehouseId: string; productId: string; quantityDelta: number; movementType: string; referenceId: string; occurredAt: string }
export const products = (token: string, branchId: string, search = '', page = 0, signal?: AbortSignal) => api<Page<Product>>(`/products?branchId=${branchId}&search=${encodeURIComponent(search)}&page=${page}`, token, { signal })
export const createProduct = (token: string, branchId: string, data: { sku: string; barcode?: string; name: string; price: number; currency: string; reorderLevel: number; taxRate: number }) => api<Product>(`/products?branchId=${branchId}`, token, { method: 'POST', body: JSON.stringify(data) })
export const stockMovements = (token: string, warehouseId: string, signal?: AbortSignal) => api<Page<StockMovement>>(`/warehouses/${warehouseId}/movements`, token, { signal })
export const changeStock = (token: string, warehouseId: string, kind: 'receipts' | 'issues', data: { productId: string; quantity: number }) => api<{ movementId: string; quantity: number }>(`/warehouses/${warehouseId}/${kind}`, token, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(data) })
export const transferStock = (token: string, warehouseId: string, data: { productId: string; destinationWarehouseId: string; quantity: number }) => api<{ movementId: string; quantity: number }>(`/warehouses/${warehouseId}/transfers`, token, { method: 'POST', headers: { 'Idempotency-Key': crypto.randomUUID() }, body: JSON.stringify(data) })
