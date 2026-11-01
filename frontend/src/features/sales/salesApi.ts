import { api } from '../../services/api'
export type Sale={id:string;branchId:string;warehouseId:string;customerId:string|null;status:'COMPLETED'|'PARTIALLY_REFUNDED'|'REFUNDED';currency:string;subtotal:number;discountTotal:number;taxTotal:number;total:number;createdBy:string;completedAt:string}
export type SaleLine={id:string;productId:string;productName:string;quantity:number;unitPrice:number;discountRate:number;taxRate:number;subtotal:number;discountAmount:number;taxAmount:number;total:number}
export type Refund={id:string;saleId:string;status:'PENDING_APPROVAL'|'COMPLETED'|'REJECTED';amount:number;reason:string;requestedBy:string;approvedBy:string|null;requestedAt:string;approvedAt:string|null}
export const sales=(token:string,branchId:string,signal?:AbortSignal)=>api<Sale[]>(`/sales?branchId=${branchId}`,token,{signal})
export const saleLines=(token:string,branchId:string,id:string,signal?:AbortSignal)=>api<SaleLine[]>(`/sales/${id}/items?branchId=${branchId}`,token,{signal})
export const checkoutCash=(token:string,branchId:string,data:{warehouseId:string;customerId?:string|null;items:{productId:string;quantity:number;discountRate:number}[]})=>api<Sale>(`/sales/cash?branchId=${branchId}`,token,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()},body:JSON.stringify(data)})
export const refundCash=(token:string,branchId:string,id:string)=>api<Sale>(`/sales/${id}/refund?branchId=${branchId}`,token,{method:'POST',headers:{'Idempotency-Key':crypto.randomUUID()}})
export const refundHistory=(token:string,branchId:string,id:string,signal?:AbortSignal)=>api<Refund[]>(`/sales/${id}/refunds?branchId=${branchId}`,token,{signal})
export const requestPartialRefund=(token:string,branchId:string,id:string,data:{reason:string;items:{saleItemId:string;quantity:number;restock:boolean}[]},key:string)=>
  api<Refund>(`/sales/${id}/refund-requests?branchId=${branchId}`,token,{method:'POST',headers:{'Idempotency-Key':key},body:JSON.stringify(data)})
export const decideRefund=(token:string,branchId:string,id:string,refundId:string,decision:'approve'|'reject')=>
  api<Refund>(`/sales/${id}/refunds/${refundId}/${decision}?branchId=${branchId}`,token,{method:'POST'})
export const salePermissions=(token:string,branchId:string,signal?:AbortSignal)=>api<{permissions:string[]}>(`/me?branchId=${branchId}`,token,{signal})
