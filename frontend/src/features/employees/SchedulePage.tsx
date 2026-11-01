import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { CalendarDays } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from '../organization/BranchContext'
import { employees } from './employeesApi'
import { addAvailability, availability, createShift, createTemplate, ownScheduledEmployee, schedulePermissions, shiftAction, shifts, templates } from './schedulingApi'

const dateValue = (date: Date) => date.toISOString().slice(0, 10)
const today = new Date()
const initialFrom = dateValue(new Date(today.getTime() - 7 * 86400000))
const initialTo = dateValue(new Date(today.getTime() + 30 * 86400000))

export function SchedulePage() {
  const { session } = useAuth()
  const { selectedId } = useBranches()
  const token = session!.access_token
  const client = useQueryClient()
  const [from, setFrom] = useState(initialFrom)
  const [to, setTo] = useState(initialTo)
  const [employeeId, setEmployeeId] = useState('')
  const [templateId, setTemplateId] = useState('')
  const [shiftStart, setShiftStart] = useState('')
  const [shiftEnd, setShiftEnd] = useState('')
  const [templateCode, setTemplateCode] = useState('')
  const [templateName, setTemplateName] = useState('')
  const [templateStart, setTemplateStart] = useState('09:00')
  const [templateEnd, setTemplateEnd] = useState('17:00')
  const [breakMinutes, setBreakMinutes] = useState('60')
  const [weekday, setWeekday] = useState('1')
  const [availableFrom, setAvailableFrom] = useState('09:00')
  const [availableTo, setAvailableTo] = useState('17:00')
  const [message, setMessage] = useState('')

  const permissions = useQuery({ queryKey: ['schedule-permissions', selectedId], queryFn: ({ signal }) => schedulePermissions(token, selectedId, signal), enabled: Boolean(selectedId) })
  const canManage = permissions.data?.permissions.includes('SCHEDULE_MANAGE') ?? false
  const canApprove = permissions.data?.permissions.includes('SCHEDULE_APPROVE') ?? false
  const canAvailability = permissions.data?.permissions.includes('SCHEDULE_AVAILABILITY') ?? false
  const employeesQuery = useQuery({ queryKey: ['employees', selectedId, ''], queryFn: ({ signal }) => employees(token, selectedId, '', signal), enabled: Boolean(selectedId && canManage) })
  const ownEmployeeQuery = useQuery({ queryKey: ['scheduled-employee', selectedId], queryFn: ({ signal }) => ownScheduledEmployee(token, selectedId, signal), enabled: Boolean(selectedId && permissions.data?.permissions.includes('SCHEDULE_SELF_VIEW')) })
  const ownEmployeeId = employeesQuery.data?.find(item => item.userId === session!.user.id)?.id ?? ownEmployeeQuery.data
  const templatesQuery = useQuery({ queryKey: ['shift-templates', selectedId], queryFn: ({ signal }) => templates(token, selectedId, signal), enabled: Boolean(selectedId && canManage) })
  const shiftsQuery = useQuery({ queryKey: ['work-shifts', selectedId, from, to], queryFn: ({ signal }) => shifts(token, selectedId, new Date(`${from}T00:00:00`).toISOString(), new Date(`${to}T23:59:59`).toISOString(), signal), enabled: Boolean(selectedId && from && to && to >= from) })
  const availabilityQuery = useQuery({ queryKey: ['availability', selectedId, ownEmployeeId], queryFn: ({ signal }) => availability(token, selectedId, ownEmployeeId!, signal), enabled: Boolean(selectedId && ownEmployeeId) })
  const refresh = () => { client.invalidateQueries({ queryKey: ['work-shifts', selectedId] }); client.invalidateQueries({ queryKey: ['shift-templates', selectedId] }) }
  const create = useMutation({ mutationFn: () => createShift(token, selectedId, { employeeId, templateId: templateId || null, startAt: new Date(shiftStart).toISOString(), endAt: new Date(shiftEnd).toISOString() }), onSuccess: () => { refresh(); setMessage('Draft shift created'); setShiftStart(''); setShiftEnd('') } })
  const action = useMutation({ mutationFn: ({ id, kind }: { id: string; kind: 'approve' | 'cancel' }) => shiftAction(token, selectedId, id, kind), onSuccess: () => { refresh(); setMessage('Shift updated') } })
  const newTemplate = useMutation({ mutationFn: () => createTemplate(token, selectedId, { code: templateCode, name: templateName, startTime: templateStart, endTime: templateEnd, breakMinutes: Number(breakMinutes) }), onSuccess: () => { refresh(); setMessage('Shift template created'); setTemplateCode(''); setTemplateName('') } })
  const newAvailability = useMutation({ mutationFn: () => addAvailability(token, selectedId, ownEmployeeId!, { weekday: Number(weekday), startTime: availableFrom, endTime: availableTo }), onSuccess: () => { client.invalidateQueries({ queryKey: ['availability', selectedId, ownEmployeeId] }); setMessage('Availability added') } })

  return <div className="erp-page"><div className="page-intro"><div><div className="eyebrow">PEOPLE</div><h1>Work schedules</h1><p>Plan branch shifts and review availability. Overlapping active assignments are blocked by the API.</p></div></div>
    {message && <div role="status" className="form-success">{message}</div>}
    {permissions.error && <div role="alert" className="form-alert">{permissions.error.message}</div>}
    <div className="report-filters"><label>From<input type="date" value={from} onChange={event => setFrom(event.target.value)} /></label><label>To<input type="date" value={to} onChange={event => setTo(event.target.value)} /></label></div>
    <section className="work-card"><div className="work-card-header"><CalendarDays size={20}/><h2>Branch shift calendar</h2></div>
      {shiftsQuery.isLoading && <p className="empty-copy">Loading shifts…</p>}{shiftsQuery.error && <div role="alert" className="form-alert">{shiftsQuery.error.message}</div>}
      {shiftsQuery.data?.length ? <ul className="entity-list">{shiftsQuery.data.map(shift => <li key={shift.id}><div><strong>{employeesQuery.data?.find(item => item.id === shift.employeeId)?.fullName ?? shift.employeeId.slice(0, 8)}</strong><small>{new Date(shift.startAt).toLocaleString()} – {new Date(shift.endAt).toLocaleString()} · {shift.status}</small></div><div className="list-actions">{shift.status === 'DRAFT' && canApprove && shift.createdBy !== session!.user.id && <Button variant="outline" disabled={action.isPending} onClick={() => action.mutate({ id: shift.id, kind: 'approve' })}>Approve</Button>}{shift.status !== 'CANCELLED' && canManage && <Button variant="outline" disabled={action.isPending} onClick={() => { if (window.confirm('Cancel this shift?')) action.mutate({ id: shift.id, kind: 'cancel' }) }}>Cancel</Button>}</div></li>)}</ul> : !shiftsQuery.isLoading && <p className="empty-copy">No shifts in this date range.</p>}
      {action.error && <div role="alert" className="form-alert">{action.error.message}</div>}
    </section>
    <div className="form-grid">
      {canManage && <section className="work-card"><div className="work-card-header"><CalendarDays size={20}/><h2>Assign shift</h2></div><form className="work-form" onSubmit={event => { event.preventDefault(); setMessage(''); create.mutate() }}><label>Employee<select required value={employeeId} onChange={event => setEmployeeId(event.target.value)}><option value="">Select employee</option>{employeesQuery.data?.filter(item => item.status === 'ACTIVE').map(item => <option key={item.id} value={item.id}>{item.fullName}</option>)}</select></label><label>Template (optional)<select value={templateId} onChange={event => setTemplateId(event.target.value)}><option value="">No template</option>{templatesQuery.data?.filter(item => item.status === 'ACTIVE').map(item => <option key={item.id} value={item.id}>{item.name}</option>)}</select></label><label>Start<input required type="datetime-local" value={shiftStart} onChange={event => setShiftStart(event.target.value)} /></label><label>End<input required type="datetime-local" value={shiftEnd} onChange={event => setShiftEnd(event.target.value)} /></label>{create.error && <div role="alert" className="form-alert">{create.error.message}</div>}<Button type="submit" disabled={!selectedId || create.isPending}>Create draft shift</Button></form></section>}
      {canManage && <section className="work-card"><div className="work-card-header"><CalendarDays size={20}/><h2>Shift templates</h2></div>{templatesQuery.data?.length ? <ul className="entity-list">{templatesQuery.data.map(item => <li key={item.id}><div><strong>{item.name}</strong><small>{item.code} · {item.startTime}–{item.endTime} · {item.breakMinutes} min break</small></div></li>)}</ul> : <p className="empty-copy">No templates yet.</p>}<form className="work-form" onSubmit={event => { event.preventDefault(); newTemplate.mutate() }}><label>Code<input required value={templateCode} onChange={event => setTemplateCode(event.target.value)} /></label><label>Name<input required value={templateName} onChange={event => setTemplateName(event.target.value)} /></label><div className="field-row"><label>Start<input required type="time" value={templateStart} onChange={event => setTemplateStart(event.target.value)} /></label><label>End<input required type="time" value={templateEnd} onChange={event => setTemplateEnd(event.target.value)} /></label></div><label>Break minutes<input required type="number" min="0" max="480" value={breakMinutes} onChange={event => setBreakMinutes(event.target.value)} /></label>{newTemplate.error && <div role="alert" className="form-alert">{newTemplate.error.message}</div>}<Button type="submit" disabled={newTemplate.isPending}>Create template</Button></form></section>}
      {canAvailability && ownEmployeeId && <section className="work-card"><div className="work-card-header"><CalendarDays size={20}/><h2>My availability</h2></div>{availabilityQuery.data?.length ? <ul className="entity-list">{availabilityQuery.data.map(item => <li key={item.id}><div><strong>Weekday {item.weekday}</strong><small>{item.startTime}–{item.endTime}</small></div></li>)}</ul> : <p className="empty-copy">No availability declared.</p>}<form className="work-form" onSubmit={event => { event.preventDefault(); newAvailability.mutate() }}><label>Day<select value={weekday} onChange={event => setWeekday(event.target.value)}>{['Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday', 'Sunday'].map((day, index) => <option key={day} value={index + 1}>{day}</option>)}</select></label><div className="field-row"><label>From<input type="time" value={availableFrom} onChange={event => setAvailableFrom(event.target.value)} /></label><label>To<input type="time" value={availableTo} onChange={event => setAvailableTo(event.target.value)} /></label></div>{newAvailability.error && <div role="alert" className="form-alert">{newAvailability.error.message}</div>}<Button type="submit" disabled={newAvailability.isPending}>Add availability</Button></form></section>}
    </div>
  </div>
}
