import { api, apiUrl } from '../../services/api'
export type UserProfile={id:string;email:string;displayName:string;status:string}
export type Role={id:string;code:string;description:string;systemRole:boolean}
export const users=(token:string,signal?:AbortSignal)=>api<UserProfile[]>('/admin/users',token,{signal})
export const roles=(token:string,signal?:AbortSignal)=>api<Role[]>('/admin/roles',token,{signal})
export const permissions=(token:string,signal?:AbortSignal)=>api<string[]>('/admin/permissions',token,{signal})
export const createUser=(token:string,data:{email:string;displayName:string;temporaryPassword:string})=>api<UserProfile>('/admin/users',token,{method:'POST',body:JSON.stringify(data)})
export const createRole=(token:string,data:{code:string;description:string})=>api<Role>('/admin/roles',token,{method:'POST',body:JSON.stringify(data)})
export const assignRole=(token:string,data:{userId:string;roleCode:string;branchId:string|null})=>api<unknown>('/admin/assignments',token,{method:'POST',body:JSON.stringify(data)})
export const setUserStatus=(token:string,id:string,status:'ACTIVE'|'INACTIVE')=>api<UserProfile>(`/admin/users/${id}/status`,token,{method:'PUT',body:JSON.stringify({status})})
export const resetUserPassword=async(token:string,id:string,temporaryPassword:string)=>{const response=await fetch(apiUrl(`/admin/users/${id}/password`),{method:'PUT',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({temporaryPassword})});if(!response.ok)throw new Error(`Password reset failed (${response.status})`)}
export const grantPermission=async(token:string,id:string,permissionCode:string)=>{const response=await fetch(apiUrl(`/admin/roles/${id}/permissions`),{method:'POST',headers:{Authorization:`Bearer ${token}`,'Content-Type':'application/json'},body:JSON.stringify({permissionCode})});if(!response.ok)throw new Error(`Permission grant failed (${response.status})`)}
