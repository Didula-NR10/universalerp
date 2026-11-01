import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Bell } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { markRead, notifications } from './notificationsApi'

export function NotificationsPage() {
  const {session}=useAuth();const token=session!.access_token;const client=useQueryClient()
  const query=useQuery({queryKey:['notifications'],queryFn:({signal})=>notifications(token,signal)})
  const mark=useMutation({mutationFn:(id:string)=>markRead(token,id),onSuccess:()=>client.invalidateQueries({queryKey:['notifications']})})
  return <div className="erp-page"><div className="page-intro"><div><div className="eyebrow">ACTIVITY</div><h1>Notifications</h1><p>Stock alerts and other business updates for your account.</p></div></div><section className="work-card"><div className="work-card-header"><Bell size={20}/><h2>Recent notifications</h2></div>{query.isLoading&&<p className="empty-copy">Loading notifications…</p>}{query.error&&<div role="alert" className="form-alert">{query.error.message}</div>}{query.data?.length?<ul className="entity-list">{query.data.map(item=><li key={item.id}><div><strong>{item.title} {!item.readAt&&<span className="soon-dot"/>}</strong><small>{item.message}</small><small>{new Date(item.createdAt).toLocaleString()}</small></div>{!item.readAt&&<Button variant="outline" disabled={mark.isPending} onClick={()=>mark.mutate(item.id)}>Mark read</Button>}</li>)}</ul>:!query.isLoading&&<p className="empty-copy">No notifications yet.</p>}{mark.error&&<div role="alert" className="form-alert">{mark.error.message}</div>}</section></div>
}
