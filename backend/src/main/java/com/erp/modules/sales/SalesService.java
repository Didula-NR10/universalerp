package com.erp.modules.sales;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import com.erp.modules.inventory.StockLedger;
import com.erp.modules.organization.OrganizationDirectory;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class SalesService {
    record NewLine(UUID productId,BigDecimal quantity,BigDecimal discountRate) {}
    record SaleView(UUID id,UUID branchId,UUID warehouseId,UUID customerId,String status,String currency,
                    BigDecimal subtotal,BigDecimal discountTotal,BigDecimal taxTotal,BigDecimal total,UUID createdBy,Instant completedAt) {}
    record LineView(UUID id,UUID productId,String productName,BigDecimal quantity,BigDecimal unitPrice,BigDecimal discountRate,
                    BigDecimal taxRate,BigDecimal subtotal,BigDecimal discountAmount,BigDecimal taxAmount,BigDecimal total) {}
    record ProductPrice(UUID id,String name,BigDecimal price,BigDecimal taxRate,String currency) {}
    record PreparedLine(NewLine request,ProductPrice product,SalesCalculator.Line amounts) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final OrganizationDirectory organization;
    private final StockLedger stock;
    SalesService(JdbcTemplate jdbc,AccessService access,OrganizationDirectory organization,StockLedger stock) {
        this.jdbc=jdbc;this.access=access;this.organization=organization;this.stock=stock;
    }
    private SaleView mapSale(ResultSet rs,int row) throws SQLException {
        return new SaleView(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),
                rs.getObject("warehouse_id",UUID.class),rs.getObject("customer_id",UUID.class),
                rs.getString("status"),rs.getString("currency").trim(),rs.getBigDecimal("subtotal"),
                rs.getBigDecimal("discount_total"),rs.getBigDecimal("tax_total"),rs.getBigDecimal("total"),
                rs.getObject("created_by",UUID.class),rs.getTimestamp("completed_at").toInstant());
    }
    @Transactional(readOnly=true)
    List<SaleView> list(Jwt jwt,UUID branchId,int limit,int offset) {
        access.require(jwt,"SALE_VIEW",branchId);
        return jdbc.query("SELECT * FROM sale WHERE branch_id=? ORDER BY completed_at DESC LIMIT ? OFFSET ?",this::mapSale,branchId,limit,offset);
    }
    @Transactional(readOnly=true)
    SaleView detail(Jwt jwt,UUID branchId,UUID id) {
        access.require(jwt,"SALE_VIEW",branchId);return saleForBranch(id,branchId,false);
    }
    @Transactional(readOnly=true)
    List<LineView> lines(Jwt jwt,UUID branchId,UUID saleId) {
        access.require(jwt,"SALE_VIEW",branchId);saleForBranch(saleId,branchId,false);
        return jdbc.query("SELECT * FROM sale_item WHERE sale_id=? ORDER BY id",(rs,row) ->
                new LineView(rs.getObject("id",UUID.class),rs.getObject("product_id",UUID.class),rs.getString("product_name"),
                        rs.getBigDecimal("quantity"),rs.getBigDecimal("unit_price"),rs.getBigDecimal("discount_rate"),
                        rs.getBigDecimal("tax_rate"),rs.getBigDecimal("subtotal"),rs.getBigDecimal("discount_amount"),
                        rs.getBigDecimal("tax_amount"),rs.getBigDecimal("total")),saleId);
    }
    @Transactional
    SaleView completeCashSale(Jwt jwt,UUID branchId,UUID warehouseId,UUID customerId,UUID key,List<NewLine> items) {
        access.require(jwt,"SALE_CREATE",branchId);
        if (items==null || items.isEmpty()) throw new IllegalArgumentException("Sale needs items");
        String requestHash=hash(branchId+"|"+warehouseId+"|"+customerId+"|"+items);
        List<UUID> prior=jdbc.query("SELECT id,branch_id,request_hash FROM sale WHERE idempotency_key=?",(rs,row) -> {
            if (!branchId.equals(rs.getObject("branch_id",UUID.class)) || !requestHash.equals(rs.getString("request_hash").trim()))
                throw new IllegalArgumentException("Idempotency key used for different sale");
            return rs.getObject("id",UUID.class);
        },key);
        if (!prior.isEmpty()) return saleForBranch(prior.get(0),branchId,false);
        if (!organization.branchForWarehouse(warehouseId).equals(branchId)) throw new IllegalArgumentException("Warehouse is outside branch");
        if (customerId!=null) {
            Integer count=jdbc.queryForObject("SELECT count(*) FROM business_contact WHERE id=? AND branch_id=? AND contact_type='CUSTOMER' AND status='ACTIVE'",Integer.class,customerId,branchId);
            if (count==null || count==0) throw new EntityNotFoundException("Active customer not found");
        }
        List<PreparedLine> prepared=new ArrayList<>(); String currency=null;
        BigDecimal subtotal=BigDecimal.ZERO,discount=BigDecimal.ZERO,tax=BigDecimal.ZERO,total=BigDecimal.ZERO;
        for (NewLine item:items) {
            if (item.productId()==null) throw new IllegalArgumentException("Product required");
            List<ProductPrice> products=jdbc.query("SELECT id,name,price,tax_rate,currency FROM product WHERE id=? AND status='ACTIVE'",(rs,row) ->
                    new ProductPrice(rs.getObject("id",UUID.class),rs.getString("name"),rs.getBigDecimal("price"),rs.getBigDecimal("tax_rate"),rs.getString("currency").trim()),item.productId());
            if (products.isEmpty()) throw new EntityNotFoundException("Active product not found");
            ProductPrice product=products.get(0);
            if (currency!=null && !currency.equals(product.currency())) throw new IllegalArgumentException("Mixed currencies are unsupported");
            currency=product.currency();
            if (item.discountRate()!=null && item.discountRate().signum()>0) access.require(jwt,"SALE_DISCOUNT",branchId);
            SalesCalculator.Line amounts=SalesCalculator.calculate(item.quantity(),product.price(),item.discountRate(),product.taxRate());
            prepared.add(new PreparedLine(item,product,amounts));
            subtotal=subtotal.add(amounts.subtotal());discount=discount.add(amounts.discount());
            tax=tax.add(amounts.tax());total=total.add(amounts.total());
        }
        UUID saleId=UUID.randomUUID();
        int inserted=jdbc.update("""
                INSERT INTO sale(id,branch_id,warehouse_id,customer_id,idempotency_key,request_hash,status,currency,
                subtotal,discount_total,tax_total,total,created_by)
                VALUES (?,?,?,?,?,?,'COMPLETED',?,?,?,?,?,?) ON CONFLICT (idempotency_key) DO NOTHING
                """,saleId,branchId,warehouseId,customerId,key,requestHash,currency,subtotal,discount,tax,total,access.userId(jwt));
        if (inserted==0) {
            List<UUID> existing=jdbc.query("SELECT id,branch_id,request_hash FROM sale WHERE idempotency_key=?",(rs,row) -> {
                if (!branchId.equals(rs.getObject("branch_id",UUID.class)) || !requestHash.equals(rs.getString("request_hash").trim()))
                    throw new IllegalArgumentException("Idempotency key used for different sale");
                return rs.getObject("id",UUID.class);
            },key);
            if (existing.isEmpty()) throw new IllegalStateException("Sale idempotency lookup failed");
            return saleForBranch(existing.get(0),branchId,false);
        }
        prepared.sort(Comparator.comparing(line -> line.product().id()));
        for (PreparedLine line:prepared) {
            UUID lineId=UUID.randomUUID();
            jdbc.update("""
                    INSERT INTO sale_item(id,sale_id,product_id,product_name,quantity,unit_price,discount_rate,tax_rate,
                    subtotal,discount_amount,tax_amount,total) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
                    """,lineId,saleId,line.product().id(),line.product().name(),line.request().quantity(),line.product().price(),
                    line.request().discountRate(),line.product().taxRate(),line.amounts().subtotal(),
                    line.amounts().discount(),line.amounts().tax(),line.amounts().total());
            stock.issueSale(jwt,warehouseId,line.product().id(),line.request().quantity(),lineId);
        }
        jdbc.update("INSERT INTO sale_payment(id,sale_id,method,amount,status) VALUES (?,?,'CASH',?,'RECEIVED')",
                UUID.randomUUID(),saleId,total);
        audit(jwt,branchId,"SALE_COMPLETED",saleId);
        return saleForBranch(saleId,branchId,false);
    }
    @Transactional
    SaleView refundCashSale(Jwt jwt,UUID branchId,UUID saleId,UUID key) {
        access.require(jwt,"SALE_REFUND",branchId);
        SaleView sale=saleForBranch(saleId,branchId,true);
        if (sale.status().equals("REFUNDED")) {
            Integer count=jdbc.queryForObject("SELECT count(*) FROM sale_refund WHERE sale_id=? AND idempotency_key=?",Integer.class,saleId,key);
            if (count!=null && count>0) return sale;
            throw new IllegalArgumentException("Sale already refunded");
        }
        if (!sale.status().equals("COMPLETED")) throw new IllegalArgumentException("Sale is not refundable");
        Integer pending=jdbc.queryForObject("SELECT count(*) FROM sale_refund WHERE sale_id=? AND status='PENDING_APPROVAL'",Integer.class,saleId);
        if (pending!=null && pending>0) throw new IllegalArgumentException("Resolve pending refund requests first");
        UUID refundId=UUID.randomUUID();
        jdbc.update("INSERT INTO sale_refund(id,sale_id,idempotency_key,amount,refunded_by,approved_by,approved_at) VALUES (?,?,?,?,?,?,now())",
                refundId,saleId,key,sale.total(),access.userId(jwt),access.userId(jwt));
        List<LineView> lines=lines(jwt,branchId,saleId);
        lines=lines.stream().sorted(Comparator.comparing(LineView::productId)).toList();
        for (LineView line:lines) {
            UUID refundLineId=UUID.randomUUID();
            jdbc.update("INSERT INTO sale_refund_item(id,sale_refund_id,sale_item_id,quantity,amount) VALUES (?,?,?,?,?)",
                    refundLineId,refundId,line.id(),line.quantity(),line.total());
            stock.receiveReturn(jwt,sale.warehouseId(),line.productId(),line.quantity(),refundLineId);
        }
        jdbc.update("UPDATE sale_payment SET status='REFUNDED' WHERE sale_id=?",saleId);
        jdbc.update("UPDATE sale SET status='REFUNDED' WHERE id=?",saleId);
        audit(jwt,branchId,"SALE_REFUNDED",saleId);
        return saleForBranch(saleId,branchId,false);
    }
    private SaleView saleForBranch(UUID id,UUID branchId,boolean lock) {
        List<SaleView> rows=jdbc.query("SELECT * FROM sale WHERE id=? AND branch_id=?"+(lock ? " FOR UPDATE" : ""),this::mapSale,id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Sale not found");
        return rows.get(0);
    }
    private String hash(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException("SHA-256 unavailable",ex); }
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'SALE',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
