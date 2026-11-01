import { api } from '../../services/api'
export type PurchaseOrder = { id:string; branchId:string; supplierId:string; warehouseId:string; status:string; currency:string; total:number; createdBy:string }
export type PurchaseItem = { id:string; productId:string; orderedQuantity:number; receivedQuantity:number; unitCost:number }
export type Receipt = { id:string; purchaseOrderId:string; orderStatus:string }
export const purchases=(token:string,branchId:string,signal?:AbortSignal)=>api<PurchaseOrder[]>(`/purchases?branchId=${branchId}`,token,{signal})
export const purchaseItems=(token:string,branchId:string,id:string,signal?:AbortSignal)=>api<PurchaseItem[]>(`/purchases/${id}/items?branchId=${branchId}`,token,{signal})
export const createPurchase=(token:string,branchId:string,data:{supplierId:string;warehouseId:string;currency:string;items:{productId:string;quantity:number;unitCost:number}[]})=>api<PurchaseOrder>(`/purchases?branchId=${branchId}`,token,{method:'POST',body:JSON.stringify(data)})
export const purchaseAction=(token:string,branchId:string,id:string,action:'submit'|'approve'|'reject'|'cancel')=>api<PurchaseOrder>(`/purchases/${id}/${action}?branchId=${branchId}`,token,{method:'POST'})
export const receivePurchase=(token:string,branchId:string,id:string,items:{orderItemId:string;quantity:number}[])=>api<Receipt>(`/purchases/${id}/receipts?branchId=${branchId}`,token,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify({items})})
