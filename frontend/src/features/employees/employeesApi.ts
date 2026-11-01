import { api } from '../../services/api'
export type Employee={id:string;branchId:string;departmentId:string|null;userId:string|null;employeeNumber:string;fullName:string;email:string|null;jobPosition:string;status:string}
export type Leave={id:string;employeeId:string;startDate:string;endDate:string;reason:string;status:string;requestedBy:string}
export const employees=(token:string,branchId:string,search:string,signal?:AbortSignal)=>api<Employee[]>(`/employees?branchId=${branchId}&search=${encodeURIComponent(search)}`,token,{signal})
export const createEmployee=(token:string,branchId:string,data:{employeeNumber:string;fullName:string;email?:string;jobPosition:string;userId?:string})=>api<Employee>(`/employees?branchId=${branchId}`,token,{method:'POST',body:JSON.stringify(data)})
export const attendance=(token:string,branchId:string,id:string,date:string,action:'check-in'|'check-out')=>api<unknown>(`/employees/${id}/attendance/${action}?branchId=${branchId}&date=${date}`,token,{method:'POST'})
export const leaves=(token:string,branchId:string,signal?:AbortSignal)=>api<Leave[]>(`/employees/leave?branchId=${branchId}`,token,{signal})
export const requestLeave=(token:string,branchId:string,id:string,data:{startDate:string;endDate:string;reason:string})=>api<Leave>(`/employees/${id}/leave?branchId=${branchId}`,token,{method:'POST',body:JSON.stringify(data)})
export const decideLeave=(token:string,branchId:string,id:string,action:'approve'|'reject')=>api<Leave>(`/employees/leave/${id}/${action}?branchId=${branchId}`,token,{method:'POST'})
