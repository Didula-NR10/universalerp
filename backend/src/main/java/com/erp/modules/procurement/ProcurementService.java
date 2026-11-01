package com.erp.modules.procurement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.erp.modules.inventory.StockLedger;
import com.erp.modules.organization.OrganizationDirectory;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class ProcurementService {
    record NewItem(UUID productId,BigDecimal quantity,BigDecimal unitCost) {}
    record ReceiveItem(UUID orderItemId,BigDecimal quantity) {}
    record OrderView(UUID id,UUID branchId,UUID supplierId,UUID warehouseId,String status,String currency,BigDecimal total,UUID createdBy) {}
    record ItemView(UUID id,UUID productId,BigDecimal orderedQuantity,BigDecimal receivedQuantity,BigDecimal unitCost) {}
    record ReceiptView(UUID id,UUID purchaseOrderId,String orderStatus) {}

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final OrganizationDirectory organization;
    private final StockLedger stock;
    ProcurementService(JdbcTemplate jdbc,AccessService access,OrganizationDirectory organization,StockLedger stock) {
        this.jdbc=jdbc; this.access=access; this.organization=organization; this.stock=stock;
    }
    private OrderView mapOrder(ResultSet rs,int row) throws java.sql.SQLException {
        return new OrderView(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),
                rs.getObject("supplier_id",UUID.class),rs.getObject("warehouse_id",UUID.class),
                rs.getString("status"),rs.getString("currency"),rs.getBigDecimal("total"),rs.getObject("created_by",UUID.class));
    }
    @Transactional(readOnly=true)
    List<OrderView> list(Jwt jwt,UUID branchId,int limit,int offset) {
        access.require(jwt,"PURCHASE_VIEW",branchId);
        return jdbc.query("SELECT * FROM purchase_order WHERE branch_id=? ORDER BY created_at DESC LIMIT ? OFFSET ?",this::mapOrder,branchId,limit,offset);
    }
    @Transactional(readOnly=true)
    List<ItemView> items(Jwt jwt,UUID branchId,UUID orderId) {
        access.require(jwt,"PURCHASE_VIEW",branchId); orderForBranch(orderId,branchId,false);
        return jdbc.query("SELECT * FROM purchase_order_item WHERE purchase_order_id=? ORDER BY id",(rs,row) ->
                new ItemView(rs.getObject("id",UUID.class),rs.getObject("product_id",UUID.class),
                        rs.getBigDecimal("ordered_quantity"),rs.getBigDecimal("received_quantity"),rs.getBigDecimal("unit_cost")),orderId);
    }
    @Transactional
    OrderView create(Jwt jwt,UUID branchId,UUID supplierId,UUID warehouseId,String currency,List<NewItem> items) {
        access.require(jwt,"PURCHASE_CREATE",branchId);
        if (!organization.branchForWarehouse(warehouseId).equals(branchId)) throw new IllegalArgumentException("Warehouse is outside branch");
        Integer supplierCount=jdbc.queryForObject("SELECT count(*) FROM business_contact WHERE id=? AND branch_id=? AND contact_type='SUPPLIER' AND status='ACTIVE'",Integer.class,supplierId,branchId);
        if (supplierCount==null || supplierCount==0) throw new EntityNotFoundException("Active supplier not found");
        if (items==null || items.isEmpty()) throw new IllegalArgumentException("Purchase order needs items");
        BigDecimal total=BigDecimal.ZERO;
        for (NewItem item:items) {
            validQuantity(item.quantity()); validMoney(item.unitCost());
            Integer productCount=jdbc.queryForObject("SELECT count(*) FROM product WHERE id=? AND status='ACTIVE'",Integer.class,item.productId());
            if (productCount==null || productCount==0) throw new EntityNotFoundException("Active product not found");
            total=total.add(item.unitCost().multiply(item.quantity()).setScale(2,RoundingMode.HALF_UP));
        }
        UUID id=UUID.randomUUID(); UUID actor=access.userId(jwt);
        jdbc.update("INSERT INTO purchase_order(id,branch_id,supplier_id,warehouse_id,status,currency,total,created_by) VALUES (?,?,?,?,'DRAFT',?,?,?)",
                id,branchId,supplierId,warehouseId,currency,total,actor);
        for (NewItem item:items) jdbc.update("INSERT INTO purchase_order_item(id,purchase_order_id,product_id,ordered_quantity,unit_cost) VALUES (?,?,?,?,?)",
                UUID.randomUUID(),id,item.productId(),item.quantity(),item.unitCost());
        audit(jwt,branchId,"PURCHASE_CREATED",id);
        return orderForBranch(id,branchId,false);
    }
    @Transactional
    OrderView submit(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"PURCHASE_CREATE",branchId);
        OrderView order=orderForBranch(id,branchId,true);
        if (!order.createdBy().equals(access.userId(jwt))) throw new AccessDeniedException("Only creator can submit purchase order");
        jdbc.update("UPDATE purchase_order SET status=?,updated_at=now(),version=version+1 WHERE id=?",PurchaseWorkflow.submit(order.status()),id);
        audit(jwt,branchId,"PURCHASE_SUBMITTED",id);
        return orderForBranch(id,branchId,false);
    }
    @Transactional
    OrderView approve(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"PURCHASE_APPROVE",branchId);
        OrderView order=orderForBranch(id,branchId,true);
        if (order.createdBy().equals(access.userId(jwt))) throw new AccessDeniedException("Creator cannot approve own purchase order");
        jdbc.update("UPDATE purchase_order SET status=?,approved_by=?,updated_at=now(),version=version+1 WHERE id=?",
                PurchaseWorkflow.approve(order.status()),access.userId(jwt),id);
        audit(jwt,branchId,"PURCHASE_APPROVED",id);
        return orderForBranch(id,branchId,false);
    }
    @Transactional
    OrderView reject(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"PURCHASE_APPROVE",branchId);
        OrderView order=orderForBranch(id,branchId,true);
        if (order.createdBy().equals(access.userId(jwt))) throw new AccessDeniedException("Creator cannot reject own purchase order");
        jdbc.update("UPDATE purchase_order SET status=?,approved_by=?,updated_at=now(),version=version+1 WHERE id=?",
                PurchaseWorkflow.reject(order.status()),access.userId(jwt),id);
        audit(jwt,branchId,"PURCHASE_REJECTED",id);
        return orderForBranch(id,branchId,false);
    }
    @Transactional
    OrderView cancel(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"PURCHASE_CREATE",branchId);
        OrderView order=orderForBranch(id,branchId,true);
        if (!order.createdBy().equals(access.userId(jwt))) throw new AccessDeniedException("Only creator can cancel purchase order");
        jdbc.update("UPDATE purchase_order SET status=?,updated_at=now(),version=version+1 WHERE id=?",
                PurchaseWorkflow.cancel(order.status()),id);
        audit(jwt,branchId,"PURCHASE_CANCELLED",id);
        return orderForBranch(id,branchId,false);
    }
    @Transactional
    ReceiptView receive(Jwt jwt,UUID branchId,UUID orderId,UUID key,List<ReceiveItem> items) {
        access.require(jwt,"PURCHASE_RECEIVE",branchId);
        OrderView order=orderForBranch(orderId,branchId,true);
        List<UUID> prior=jdbc.query("SELECT id,purchase_order_id FROM goods_receipt WHERE idempotency_key=?",(rs,row) -> {
            UUID existingOrder=rs.getObject("purchase_order_id",UUID.class);
            if (!existingOrder.equals(orderId)) throw new IllegalArgumentException("Idempotency key belongs to another order");
            return rs.getObject("id",UUID.class);
        },key);
        if (!prior.isEmpty()) return new ReceiptView(prior.get(0),orderId,order.status());
        PurchaseWorkflow.receive(order.status(),false);
        if (items==null || items.isEmpty()) throw new IllegalArgumentException("Receipt needs items");
        UUID receiptId=UUID.randomUUID();
        jdbc.update("INSERT INTO goods_receipt(id,purchase_order_id,idempotency_key,received_by) VALUES (?,?,?,?)",
                receiptId,orderId,key,access.userId(jwt));
        Set<UUID> seen=new HashSet<>();
        for (ReceiveItem item:items) {
            if (!seen.add(item.orderItemId())) throw new IllegalArgumentException("Duplicate order item");
            validQuantity(item.quantity());
            List<ItemView> rows=jdbc.query("SELECT * FROM purchase_order_item WHERE id=? AND purchase_order_id=?",(rs,row) ->
                    new ItemView(rs.getObject("id",UUID.class),rs.getObject("product_id",UUID.class),
                            rs.getBigDecimal("ordered_quantity"),rs.getBigDecimal("received_quantity"),rs.getBigDecimal("unit_cost")),item.orderItemId(),orderId);
            if (rows.isEmpty()) throw new EntityNotFoundException("Order item not found");
            ItemView row=rows.get(0);
            BigDecimal next=row.receivedQuantity().add(item.quantity());
            if (next.compareTo(row.orderedQuantity())>0) throw new IllegalArgumentException("Receipt exceeds ordered quantity");
            UUID receiptLineId=UUID.randomUUID();
            jdbc.update("INSERT INTO goods_receipt_item(id,goods_receipt_id,purchase_order_item_id,quantity) VALUES (?,?,?,?)",
                    receiptLineId,receiptId,row.id(),item.quantity());
            stock.receivePurchase(jwt,order.warehouseId(),row.productId(),item.quantity(),receiptLineId);
            jdbc.update("UPDATE purchase_order_item SET received_quantity=? WHERE id=?",next,row.id());
        }
        Integer remaining=jdbc.queryForObject("SELECT count(*) FROM purchase_order_item WHERE purchase_order_id=? AND received_quantity<ordered_quantity",Integer.class,orderId);
        String nextStatus=PurchaseWorkflow.receive(order.status(),remaining!=null && remaining==0);
        jdbc.update("UPDATE purchase_order SET status=?,updated_at=now(),version=version+1 WHERE id=?",nextStatus,orderId);
        audit(jwt,branchId,"GOODS_RECEIVED",receiptId);
        return new ReceiptView(receiptId,orderId,nextStatus);
    }
    private OrderView orderForBranch(UUID id,UUID branchId,boolean lock) {
        List<OrderView> rows=jdbc.query("SELECT * FROM purchase_order WHERE id=? AND branch_id=?"+(lock ? " FOR UPDATE" : ""),this::mapOrder,id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Purchase order not found");
        return rows.get(0);
    }
    private void validQuantity(BigDecimal value) {
        if (value==null || value.signum()<=0 || value.scale()>3 || value.precision()>19) throw new IllegalArgumentException("Invalid quantity");
    }
    private void validMoney(BigDecimal value) {
        if (value==null || value.signum()<0 || value.scale()>2 || value.precision()>19) throw new IllegalArgumentException("Invalid unit cost");
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'PURCHASE_ORDER',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
