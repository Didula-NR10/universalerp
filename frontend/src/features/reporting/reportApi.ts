import { api, apiUrl } from '../../services/api'
export type Report={currency:string;salesTotal:number;expenseTotal:number;saleCount:number;purchaseCount:number;employeeCount:number;lowStockProducts:number;dailySales:{day:string;total:number;count:number}[]}
export const report=(token:string,branchId:string,from:string,to:string,currency:string,signal?:AbortSignal)=>api<Report>(`/reports/overview?branchId=${branchId}&from=${from}&to=${to}&currency=${currency}`,token,{signal})
export async function downloadSalesCsv(token:string,branchId:string,from:string,to:string,currency:string) {
  const response=await fetch(apiUrl(`/reports/sales.csv?branchId=${branchId}&from=${from}&to=${to}&currency=${currency}`),{headers:{Authorization:`Bearer ${token}`}})
  if(!response.ok)throw new Error(`CSV export failed (${response.status})`)
  const url=URL.createObjectURL(await response.blob())
  const link=document.createElement('a');link.href=url;link.download='sales-report.csv';link.click();URL.revokeObjectURL(url)
}
