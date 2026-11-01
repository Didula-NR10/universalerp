import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Truck } from 'lucide-react'
import { Button } from '../../components/ui/button'
import { useAuth } from '../auth/AuthProvider'
import { useBranches } from '../organization/BranchContext'
import { contacts } from '../contacts/contactsApi'
import { products } from '../inventory/inventoryApi'
import { warehouses } from '../organization/organizationApi'
import { createPurchase, purchaseAction, purchaseItems, purchases, receivePurchase } from './procurementApi'

type DraftItem = { productId: string; quantity: number; unitCost: number }

export function ProcurementPage() {
  const { session } = useAuth()
  const { selectedId } = useBranches()
  const token = session!.access_token
  const client = useQueryClient()
  const [supplierId, setSupplierId] = useState('')
  const [warehouseId, setWarehouseId] = useState('')
  const [productId, setProductId] = useState('')
  const [quantity, setQuantity] = useState('1')
  const [unitCost, setUnitCost] = useState('0')
  const [currency, setCurrency] = useState('USD')
  const [draftItems, setDraftItems] = useState<DraftItem[]>([])
  const [selectedOrder, setSelectedOrder] = useState('')
  const [receiveItemId, setReceiveItemId] = useState('')
  const [receiveQuantity, setReceiveQuantity] = useState('')
  const [message, setMessage] = useState('')

  const suppliers = useQuery({ queryKey: ['contacts', 'SUPPLIER', selectedId, ''], queryFn: ({ signal }) => contacts(token, 'SUPPLIER', selectedId, '', signal), enabled: Boolean(selectedId) })
  const warehouseQuery = useQuery({ queryKey: ['warehouses', selectedId], queryFn: ({ signal }) => warehouses(token, selectedId, signal), enabled: Boolean(selectedId) })
  const catalog = useQuery({ queryKey: ['products', selectedId, '', 0], queryFn: ({ signal }) => products(token, selectedId, '', 0, signal), enabled: Boolean(selectedId) })
  const orders = useQuery({ queryKey: ['purchases', selectedId], queryFn: ({ signal }) => purchases(token, selectedId, signal), enabled: Boolean(selectedId) })
  const items = useQuery({ queryKey: ['purchase-items', selectedId, selectedOrder], queryFn: ({ signal }) => purchaseItems(token, selectedId, selectedOrder, signal), enabled: Boolean(selectedId && selectedOrder) })
  const selected = orders.data?.find(order => order.id === selectedOrder)
  const receivingAllowed = selected?.status === 'APPROVED' || selected?.status === 'PARTIALLY_RECEIVED'
  const selectedItem = items.data?.find(item => item.id === receiveItemId)
  const remaining = selectedItem ? selectedItem.orderedQuantity - selectedItem.receivedQuantity : 0
  const refresh = () => { client.invalidateQueries({ queryKey: ['purchases', selectedId] }); client.invalidateQueries({ queryKey: ['purchase-items', selectedId] }); client.invalidateQueries({ queryKey: ['dashboard'] }) }
  const create = useMutation({ mutationFn: () => createPurchase(token, selectedId, { supplierId, warehouseId, currency, items: draftItems }), onSuccess: () => { refresh(); setDraftItems([]); setMessage('Purchase order created') } })
  const action = useMutation({ mutationFn: ({ id, kind }: { id: string; kind: 'submit' | 'approve' | 'reject' | 'cancel' }) => purchaseAction(token, selectedId, id, kind), onSuccess: () => { refresh(); setMessage('Purchase order updated') } })
  const receive = useMutation({ mutationFn: () => receivePurchase(token, selectedId, selectedOrder, [{ orderItemId: receiveItemId, quantity: Number(receiveQuantity) }]), onSuccess: () => { refresh(); client.invalidateQueries({ queryKey: ['movements'] }); setReceiveQuantity(''); setMessage('Goods received into inventory') } })

  const addItem = () => {
    const qty = Number(quantity), cost = Number(unitCost)
    if (!productId || !Number.isFinite(qty) || qty <= 0 || !Number.isFinite(cost) || cost < 0) return
    if (draftItems.some(item => item.productId === productId)) { setMessage('Product is already on this order'); return }
    setDraftItems(current => [...current, { productId, quantity: qty, unitCost: cost }])
    setProductId('')
    setMessage('')
  }
  return <div className="erp-page">
    <div className="page-intro"><div><div className="eyebrow">PROCUREMENT</div><h1>Purchase orders</h1><p>Create, approve, and receive orders into warehouse stock.</p></div></div>
    {message && <div role="status" className="form-success">{message}</div>}
    <div className="form-grid">
      <section className="work-card"><div className="work-card-header"><Truck size={20}/><h2>Orders</h2></div>
        {orders.isLoading && <p className="empty-copy">Loading orders…</p>}
        {orders.error && <div role="alert" className="form-alert">{orders.error.message}</div>}
        {orders.data?.length ? <ul className="entity-list">{orders.data.map(order => <li key={order.id}><div><strong>{order.currency} {order.total.toFixed(2)}</strong><small>{order.status} · {order.id.slice(0, 8)}</small></div><div className="list-actions">
          <Button variant="outline" onClick={() => { setSelectedOrder(order.id); setReceiveItemId(''); setReceiveQuantity('') }}>Details</Button>
          {order.status === 'DRAFT' && order.createdBy === session!.user.id && <Button variant="outline" disabled={action.isPending} onClick={() => action.mutate({ id: order.id, kind: 'submit' })}>Submit</Button>}
          {order.status === 'SUBMITTED' && order.createdBy !== session!.user.id && <Button variant="outline" disabled={action.isPending} onClick={() => action.mutate({ id: order.id, kind: 'approve' })}>Approve</Button>}
          {order.status === 'SUBMITTED' && order.createdBy !== session!.user.id && <Button variant="outline" disabled={action.isPending} onClick={() => { if (window.confirm('Reject this purchase order?')) action.mutate({ id: order.id, kind: 'reject' }) }}>Reject</Button>}
          {(order.status === 'DRAFT' || order.status === 'SUBMITTED') && order.createdBy === session!.user.id && <Button variant="outline" disabled={action.isPending} onClick={() => { if (window.confirm('Cancel this purchase order?')) action.mutate({ id: order.id, kind: 'cancel' }) }}>Cancel</Button>}
        </div></li>)}</ul> : !orders.isLoading && <p className="empty-copy">No purchase orders yet.</p>}
        {action.error && <div role="alert" className="form-alert">{action.error.message}</div>}
        {selectedOrder && <div className="work-form"><h3>Order items</h3>{items.isLoading && <p className="empty-copy">Loading order items…</p>}{items.error && <div role="alert" className="form-alert">{items.error.message}</div>}{items.data?.map(item => <p className="empty-copy" key={item.id}>{catalog.data?.content.find(product => product.id === item.productId)?.name ?? item.productId.slice(0, 8)}: {item.receivedQuantity} of {item.orderedQuantity} received</p>)}
          {receivingAllowed && <><label>Item to receive<select value={receiveItemId} onChange={event => { setReceiveItemId(event.target.value); setReceiveQuantity('') }}><option value="">Select item</option>{items.data?.filter(item => item.receivedQuantity < item.orderedQuantity).map(item => <option value={item.id} key={item.id}>{catalog.data?.content.find(product => product.id === item.productId)?.name ?? item.productId.slice(0, 8)} ({item.orderedQuantity - item.receivedQuantity} remaining)</option>)}</select></label><label>Quantity<input type="number" min="0.001" max={remaining} step="0.001" value={receiveQuantity} onChange={event => setReceiveQuantity(event.target.value)} /></label>{receive.error && <div role="alert" className="form-alert">{receive.error.message}</div>}<Button disabled={!receiveItemId || !receiveQuantity || Number(receiveQuantity) > remaining || receive.isPending} onClick={() => receive.mutate()}>Receive into stock</Button></>}
        </div>}
      </section>
      <section className="work-card"><div className="work-card-header"><Truck size={20}/><h2>New order</h2></div><div className="work-form"><label>Supplier<select value={supplierId} onChange={event => setSupplierId(event.target.value)}><option value="">Select supplier</option>{suppliers.data?.content.map(contact => <option key={contact.id} value={contact.id}>{contact.name}</option>)}</select></label><label>Receiving warehouse<select value={warehouseId} onChange={event => setWarehouseId(event.target.value)}><option value="">Select warehouse</option>{warehouseQuery.data?.map(warehouse => <option key={warehouse.id} value={warehouse.id}>{warehouse.name}</option>)}</select></label><label>Currency<input maxLength={3} value={currency} onChange={event => setCurrency(event.target.value.toUpperCase())} /></label><h3>Add product line</h3><label>Product<select value={productId} onChange={event => setProductId(event.target.value)}><option value="">Select product</option>{catalog.data?.content.map(product => <option key={product.id} value={product.id}>{product.name}</option>)}</select></label><div className="field-row"><label>Quantity<input type="number" min="0.001" step="0.001" value={quantity} onChange={event => setQuantity(event.target.value)} /></label><label>Unit cost<input type="number" min="0" step="0.01" value={unitCost} onChange={event => setUnitCost(event.target.value)} /></label></div><Button variant="outline" type="button" disabled={!productId} onClick={addItem}>Add line</Button>{draftItems.length ? <ul className="entity-list">{draftItems.map(item => <li key={item.productId}><div><strong>{catalog.data?.content.find(product => product.id === item.productId)?.name ?? item.productId.slice(0, 8)}</strong><small>{item.quantity} × {currency} {item.unitCost.toFixed(2)}</small></div><Button variant="ghost" onClick={() => setDraftItems(current => current.filter(line => line.productId !== item.productId))}>Remove</Button></li>)}</ul> : <p className="empty-copy">Add at least one product.</p>}{create.error && <div role="alert" className="form-alert">{create.error.message}</div>}<Button disabled={!selectedId || !supplierId || !warehouseId || !draftItems.length || create.isPending} onClick={() => { setMessage(''); create.mutate() }}>Create order</Button></div></section>
    </div>
  </div>
}
