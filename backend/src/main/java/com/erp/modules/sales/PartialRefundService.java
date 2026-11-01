package com.erp.modules.sales;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.erp.modules.inventory.StockLedger;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class PartialRefundService {
    record RequestedLine(UUID saleItemId,BigDecimal quantity,boolean restock) {}
    record RefundView(UUID id,UUID saleId,String status,BigDecimal amount,String reason,
                      UUID requestedBy,UUID approvedBy,Instant requestedAt,Instant approvedAt) {}
    record RefundLine(UUID id,UUID saleItemId,UUID productId,BigDecimal quantity,BigDecimal amount,boolean restock) {}
    private record Sale(UUID id,UUID branchId,UUID warehouseId,String status) {}
    private record SaleLine(UUID id,UUID productId,BigDecimal quantity,BigDecimal total) {}
    private record AllocatedLine(RequestedLine request,BigDecimal amount) {}

    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final StockLedger stock;
    PartialRefundService(JdbcTemplate jdbc,AccessService access,StockLedger stock) {
        this.jdbc=jdbc;this.access=access;this.stock=stock;
    }

    @Transactional(readOnly=true)
    List<RefundView> list(Jwt jwt,UUID branchId,UUID saleId) {
        access.require(jwt,"SALE_VIEW",branchId);sale(saleId,branchId,false);
        return jdbc.query("SELECT * FROM sale_refund WHERE sale_id=? ORDER BY refunded_at DESC",this::mapRefund,saleId);
    }

    @Transactional(readOnly=true)
    List<RefundLine> lines(Jwt jwt,UUID branchId,UUID saleId,UUID refundId) {
        access.require(jwt,"SALE_VIEW",branchId);sale(saleId,branchId,false);refund(refundId,saleId,false);
        return refundLines(refundId);
    }

    @Transactional
    RefundView request(Jwt jwt,UUID branchId,UUID saleId,UUID key,String reason,List<RequestedLine> requested) {
        access.require(jwt,"SALE_REFUND_REQUEST",branchId);
        Sale sale=sale(saleId,branchId,true);
        if (reason==null || reason.isBlank() || reason.length()>500 || requested==null || requested.isEmpty())
            throw new IllegalArgumentException("Refund reason and items are required");
        String cleanReason=reason.trim();
        String requestHash=hash(saleId+"|"+cleanReason+"|"+requested.stream()
                .sorted(Comparator.comparing(RequestedLine::saleItemId)).toList());
        List<RefundView> previous=jdbc.query("SELECT * FROM sale_refund WHERE idempotency_key=?",this::mapRefund,key);
        if (!previous.isEmpty()) {
            RefundView prior=previous.get(0);
            String previousHash=jdbc.queryForObject("SELECT request_hash FROM sale_refund WHERE id=?",String.class,prior.id());
            if (!prior.saleId().equals(saleId) || !requestHash.equals(previousHash==null?null:previousHash.trim()))
                throw new IllegalArgumentException("Idempotency key used for another refund request");
            return prior;
        }
        if (sale.status().equals("REFUNDED")) throw new IllegalArgumentException("Sale is already fully refunded");
        Set<UUID> seen=new HashSet<>();BigDecimal total=BigDecimal.ZERO;
        List<AllocatedLine> allocated=new java.util.ArrayList<>();
        for (RequestedLine line:requested) {
            if (line.saleItemId()==null || !seen.add(line.saleItemId())) throw new IllegalArgumentException("Duplicate or missing sale item");
            SaleLine original=saleLine(saleId,line.saleItemId());
            BigDecimal[] prior=jdbc.queryForObject("""
                    SELECT coalesce(sum(ri.quantity),0),coalesce(sum(ri.amount),0)
                    FROM sale_refund_item ri JOIN sale_refund r ON r.id=ri.sale_refund_id
                    WHERE r.sale_id=? AND ri.sale_item_id=? AND r.status IN ('PENDING_APPROVAL','COMPLETED')
                    """,(rs,row)->new BigDecimal[]{rs.getBigDecimal(1),rs.getBigDecimal(2)},saleId,line.saleItemId());
            BigDecimal amount=RefundPolicy.amount(original.quantity(),original.total(),prior[0],prior[1],line.quantity());
            total=total.add(amount);allocated.add(new AllocatedLine(line,amount));
        }
        UUID id=UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sale_refund(id,sale_id,idempotency_key,amount,refunded_by,reason,status,request_hash)
                VALUES (?,?,?,?,?,?,'PENDING_APPROVAL',?)
                """,id,saleId,key,total,access.userId(jwt),cleanReason,requestHash);
        for (AllocatedLine item:allocated) jdbc.update("""
                INSERT INTO sale_refund_item(id,sale_refund_id,sale_item_id,quantity,amount,restock)
                VALUES (?,?,?,?,?,?)
                """,UUID.randomUUID(),id,item.request().saleItemId(),item.request().quantity(),item.amount(),item.request().restock());
        audit(jwt,branchId,"SALE_REFUND_REQUESTED",id);
        return refund(id,saleId,false);
    }

    @Transactional
    RefundView decide(Jwt jwt,UUID branchId,UUID saleId,UUID refundId,boolean approve) {
        access.require(jwt,"SALE_REFUND",branchId);
        Sale sale=sale(saleId,branchId,true);
        RefundView request=refund(refundId,saleId,true);
        if (!"PENDING_APPROVAL".equals(request.status())) throw new IllegalArgumentException("Refund request is already decided");
        if (request.requestedBy().equals(access.userId(jwt))) throw new AccessDeniedException("Requester cannot decide own refund");
        if (approve) {
            if (sale.status().equals("REFUNDED")) throw new IllegalArgumentException("Sale is already fully refunded");
            for (RefundLine line:refundLines(refundId)) {
                if (line.restock()) stock.receiveReturn(jwt,sale.warehouseId(),line.productId(),line.quantity(),line.id());
            }
        }
        String status=approve?"COMPLETED":"REJECTED";
        jdbc.update("UPDATE sale_refund SET status=?,approved_by=?,approved_at=now() WHERE id=?",status,access.userId(jwt),refundId);
        if (approve) {
            Long remaining=jdbc.queryForObject("""
                    SELECT count(*) FROM sale_item si WHERE si.sale_id=?
                    AND si.quantity>coalesce((SELECT sum(ri.quantity) FROM sale_refund_item ri
                        JOIN sale_refund r ON r.id=ri.sale_refund_id
                        WHERE ri.sale_item_id=si.id AND r.status='COMPLETED'),0)
                    """,Long.class,saleId);
            String saleStatus=remaining!=null && remaining==0?"REFUNDED":"PARTIALLY_REFUNDED";
            jdbc.update("UPDATE sale SET status=? WHERE id=?",saleStatus,saleId);
            jdbc.update("UPDATE sale_payment SET status=? WHERE sale_id=?",
                    saleStatus.equals("REFUNDED")?"REFUNDED":"PARTIALLY_REFUNDED",saleId);
        }
        audit(jwt,branchId,approve?"SALE_REFUND_APPROVED":"SALE_REFUND_REJECTED",refundId);
        return refund(refundId,saleId,false);
    }

    private Sale sale(UUID id,UUID branchId,boolean lock) {
        List<Sale> rows=jdbc.query("SELECT id,branch_id,warehouse_id,status FROM sale WHERE id=? AND branch_id=?"+(lock?" FOR UPDATE":""),
                (rs,row)->new Sale(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getString(4)),id,branchId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Sale not found");
        return rows.get(0);
    }
    private SaleLine saleLine(UUID saleId,UUID id) {
        List<SaleLine> rows=jdbc.query("SELECT id,product_id,quantity,total FROM sale_item WHERE id=? AND sale_id=?",
                (rs,row)->new SaleLine(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getBigDecimal(3),rs.getBigDecimal(4)),id,saleId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Sale item not found");
        return rows.get(0);
    }
    private RefundView refund(UUID id,UUID saleId,boolean lock) {
        List<RefundView> rows=jdbc.query("SELECT * FROM sale_refund WHERE id=? AND sale_id=?"+(lock?" FOR UPDATE":""),this::mapRefund,id,saleId);
        if (rows.isEmpty()) throw new EntityNotFoundException("Refund request not found");
        return rows.get(0);
    }
    private RefundView mapRefund(ResultSet rs,int row) throws SQLException {
        Timestamp approved=rs.getTimestamp("approved_at");
        return new RefundView(rs.getObject("id",UUID.class),rs.getObject("sale_id",UUID.class),rs.getString("status"),
                rs.getBigDecimal("amount"),rs.getString("reason"),rs.getObject("refunded_by",UUID.class),
                rs.getObject("approved_by",UUID.class),rs.getTimestamp("refunded_at").toInstant(),approved==null?null:approved.toInstant());
    }
    private List<RefundLine> refundLines(UUID id) {
        return jdbc.query("""
                SELECT ri.id,ri.sale_item_id,si.product_id,ri.quantity,ri.amount,ri.restock
                FROM sale_refund_item ri JOIN sale_item si ON si.id=ri.sale_item_id WHERE ri.sale_refund_id=? ORDER BY ri.id
                """,(rs,row)->new RefundLine(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),
                rs.getBigDecimal(4),rs.getBigDecimal(5),rs.getBoolean(6)),id);
    }
    private String hash(String input) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); }
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'SALE_REFUND',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
