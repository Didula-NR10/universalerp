import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ShoppingBag } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from '../organization/BranchContext'
import { decideRefund, refundCash, refundHistory, requestPartialRefund, saleLines, salePermissions, sales } from './salesApi'

export function SalesPage() {
  const { session } = useAuth()
  const { selectedId } = useBranches()
  const token = session!.access_token
  const client = useQueryClient()
  const [selectedSale, setSelectedSale] = useState('')
  const [reason, setReason] = useState('')
  const [quantities, setQuantities] = useState<Record<string, string>>({})
  const [restock, setRestock] = useState<Record<string, boolean>>({})
  const [message, setMessage] = useState('')
  const query = useQuery({ queryKey: ['sales', selectedId], queryFn: ({ signal }) => sales(token, selectedId, signal), enabled: Boolean(selectedId) })
  const lines = useQuery({ queryKey: ['sale-lines', selectedId, selectedSale], queryFn: ({ signal }) => saleLines(token, selectedId, selectedSale, signal), enabled: Boolean(selectedId && selectedSale) })
  const history = useQuery({ queryKey: ['refund-history', selectedId, selectedSale], queryFn: ({ signal }) => refundHistory(token, selectedId, selectedSale, signal), enabled: Boolean(selectedId && selectedSale) })
  const permissionQuery = useQuery({ queryKey: ['sale-permissions', selectedId], queryFn: ({ signal }) => salePermissions(token, selectedId, signal), enabled: Boolean(selectedId) })
  const permissions = permissionQuery.data?.permissions ?? []
  const selected = query.data?.find(sale => sale.id === selectedSale)
  const refresh = () => { client.invalidateQueries({ queryKey: ['sales', selectedId] }); client.invalidateQueries({ queryKey: ['refund-history', selectedId, selectedSale] }); client.invalidateQueries({ queryKey: ['movements'] }); client.invalidateQueries({ queryKey: ['dashboard'] }) }
  const fullRefund = useMutation({ mutationFn: (id: string) => refundCash(token, selectedId, id), onSuccess: () => { refresh(); setMessage('Full cash refund recorded and stock restored') } })
  const partialRefund = useMutation({
    mutationFn: () => {
      const items = (lines.data ?? []).filter(line => Number(quantities[line.id] ?? 0) > 0).map(line => ({ saleItemId: line.id, quantity: Number(quantities[line.id]), restock: restock[line.id] !== false }))
      if (!reason.trim() || !items.length) throw new Error('Enter a reason and at least one refund quantity')
      return requestPartialRefund(token, selectedId, selectedSale, { reason: reason.trim(), items }, crypto.randomUUID())
    },
    onSuccess: () => { refresh(); setQuantities({}); setReason(''); setMessage('Refund request submitted for approval') },
  })
  const decision = useMutation({
    mutationFn: ({ id, action }: { id: string; action: 'approve' | 'reject' }) => decideRefund(token, selectedId, selectedSale, id, action),
    onSuccess: () => { refresh(); setMessage('Refund decision recorded') },
  })

  return <div className="erp-page">
    <div className="page-intro"><div><div className="eyebrow">SALES</div><h1>Cash sales and refunds</h1><p>Review sales, request partial refunds, and decide pending requests.</p></div></div>
    {message && <div role="status" className="form-success">{message}</div>}
    <section className="work-card"><div className="work-card-header"><ShoppingBag size={20}/><h2>Sales history</h2></div>
      {query.isLoading && <p className="empty-copy">Loading sales…</p>}
      {query.error && <div role="alert" className="form-alert">{query.error.message}</div>}
      {permissionQuery.error && <div role="alert" className="form-alert">{permissionQuery.error.message}</div>}
      {query.data?.length ? <ul className="entity-list">{query.data.map(sale => <li key={sale.id}><div><strong>{sale.currency} {sale.total.toFixed(2)}</strong><small>{sale.status} · {new Date(sale.completedAt).toLocaleString()} · {sale.id.slice(0, 8)}</small></div><div className="list-actions"><Button variant="outline" onClick={() => setSelectedSale(sale.id)}>Details</Button>{permissions.includes('SALE_REFUND') && sale.status === 'COMPLETED' && <Button variant="outline" disabled={fullRefund.isPending} onClick={() => { if (window.confirm('Confirm the full cash refund and restore all stock?')) fullRefund.mutate(sale.id) }}>Full refund</Button>}</div></li>)}</ul> : !query.isLoading && <p className="empty-copy">No sales yet.</p>}
      {fullRefund.error && <div role="alert" className="form-alert">{fullRefund.error.message}</div>}
    </section>
    {selected && <section className="work-card detail-card"><div className="work-card-header"><ShoppingBag size={20}/><h2>Sale lines</h2></div>
      {lines.isLoading && <p className="empty-copy">Loading lines…</p>}
      {lines.error && <div role="alert" className="form-alert">{lines.error.message}</div>}
      {lines.data?.length ? <div className="table-wrap"><table><thead><tr><th>Product</th><th>Sold</th><th>Unit price</th><th>Discount</th><th>Tax</th><th>Total</th></tr></thead><tbody>{lines.data.map(line => <tr key={line.id}><td>{line.productName}</td><td>{line.quantity}</td><td>{line.unitPrice.toFixed(2)}</td><td>{line.discountAmount.toFixed(2)}</td><td>{line.taxAmount.toFixed(2)}</td><td>{line.total.toFixed(2)}</td></tr>)}</tbody></table></div> : null}
      {permissions.includes('SALE_REFUND_REQUEST') && selected.status !== 'REFUNDED' && <form className="work-form" onSubmit={event => { event.preventDefault(); setMessage(''); partialRefund.mutate() }}><h3>Request a partial refund</h3><label>Reason<input required maxLength={500} value={reason} onChange={event => setReason(event.target.value)} /></label>{lines.data?.map(line => <div className="field-row" key={line.id}><label>{line.productName} quantity<input type="number" min="0" max={line.quantity} step="0.001" value={quantities[line.id] ?? ''} onChange={event => setQuantities(current => ({ ...current, [line.id]: event.target.value }))} /></label><label>Return to stock<input type="checkbox" checked={restock[line.id] !== false} onChange={event => setRestock(current => ({ ...current, [line.id]: event.target.checked }))} /></label></div>)}<p className="empty-copy">The server checks previous refunds and calculates the refundable amount. A different authorized user must approve this request.</p>{partialRefund.error && <div role="alert" className="form-alert">{partialRefund.error.message}</div>}<Button type="submit" disabled={partialRefund.isPending}>Submit refund request</Button></form>}
      <div className="work-form"><h3>Refund history</h3>{history.isLoading && <p className="empty-copy">Loading refunds…</p>}{history.error && <div role="alert" className="form-alert">{history.error.message}</div>}{history.data?.length ? <ul className="entity-list">{history.data.map(refund => <li key={refund.id}><div><strong>{selected.currency} {refund.amount.toFixed(2)} · {refund.status}</strong><small>{refund.reason} · {new Date(refund.requestedAt).toLocaleString()}</small></div>{refund.status === 'PENDING_APPROVAL' && refund.requestedBy !== session!.user.id && permissions.includes('SALE_REFUND') && <div className="list-actions"><Button variant="outline" disabled={decision.isPending} onClick={() => { if (window.confirm('Approve this cash refund after confirming the payout?')) decision.mutate({ id: refund.id, action: 'approve' }) }}>Approve</Button><Button variant="outline" disabled={decision.isPending} onClick={() => { if (window.confirm('Reject this refund request?')) decision.mutate({ id: refund.id, action: 'reject' }) }}>Reject</Button></div>}</li>)}</ul> : !history.isLoading && <p className="empty-copy">No refunds for this sale.</p>}{decision.error && <div role="alert" className="form-alert">{decision.error.message}</div>}</div>
    </section>}
  </div>
}
