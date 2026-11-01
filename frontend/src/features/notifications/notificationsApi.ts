import { api, apiUrl } from '../../services/api'
export type Notification={id:string;branchId:string|null;type:string;title:string;message:string;createdAt:string;readAt:string|null}
export const notifications=(token:string,signal?:AbortSignal)=>api<Notification[]>('/notifications',token,{signal})
export async function markRead(token:string,id:string) {
  const response=await fetch(apiUrl(`/notifications/${id}/read`),{method:'POST',headers:{Authorization:`Bearer ${token}`}})
  if(!response.ok)throw new Error(`Could not mark notification read (${response.status})`)
}
