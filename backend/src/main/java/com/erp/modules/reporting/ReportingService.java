package com.erp.modules.reporting;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import com.erp.security.AccessService;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class ReportingService {
    record DailySales(LocalDate day,BigDecimal total,long count) {}
    record Overview(String currency,BigDecimal salesTotal,BigDecimal expenseTotal,long saleCount,long purchaseCount,
                    long employeeCount,long lowStockProducts,List<DailySales> dailySales) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    ReportingService(JdbcTemplate jdbc,AccessService access) { this.jdbc=jdbc;this.access=access; }
    @Transactional(readOnly=true)
    Overview overview(Jwt jwt,UUID branchId,LocalDate from,LocalDate to,String currency) {
        access.require(jwt,"REPORT_VIEW",branchId);validateDates(from,to);
        Timestamp start=Timestamp.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp end=Timestamp.from(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        BigDecimal sales=jdbc.queryForObject("""
                SELECT coalesce(sum(s.total-coalesce((SELECT sum(r.amount) FROM sale_refund r
                    WHERE r.sale_id=s.id AND r.status='COMPLETED'),0)),0)
                FROM sale s WHERE s.branch_id=? AND s.currency=? AND s.completed_at>=? AND s.completed_at<?
                """,
                BigDecimal.class,branchId,currency,start,end);
        BigDecimal expenses=jdbc.queryForObject("SELECT coalesce(sum(amount),0) FROM expense WHERE branch_id=? AND currency=? AND status='APPROVED' AND created_at>=? AND created_at<?",
                BigDecimal.class,branchId,currency,start,end);
        Long saleCount=jdbc.queryForObject("SELECT count(*) FROM sale WHERE branch_id=? AND completed_at>=? AND completed_at<?",
                Long.class,branchId,start,end);
        Long purchaseCount=jdbc.queryForObject("SELECT count(*) FROM purchase_order WHERE branch_id=? AND status NOT IN ('CANCELLED','REJECTED') AND created_at>=? AND created_at<?",
                Long.class,branchId,start,end);
        Long employeeCount=jdbc.queryForObject("SELECT count(*) FROM employee WHERE branch_id=? AND status='ACTIVE'",Long.class,branchId);
        Long lowStock=jdbc.queryForObject("""
                SELECT count(*) FROM stock_balance b JOIN warehouse w ON w.id=b.warehouse_id
                JOIN product p ON p.id=b.product_id WHERE w.branch_id=? AND p.status='ACTIVE' AND b.quantity<p.reorder_level
                """,Long.class,branchId);
        List<DailySales> daily=jdbc.query("""
                SELECT (s.completed_at AT TIME ZONE 'UTC')::date AS day,
                  coalesce(sum(s.total-coalesce((SELECT sum(r.amount) FROM sale_refund r
                      WHERE r.sale_id=s.id AND r.status='COMPLETED'),0)),0) AS total,count(*) AS count
                FROM sale s WHERE s.branch_id=? AND s.currency=? AND s.completed_at>=? AND s.completed_at<?
                GROUP BY day ORDER BY day
                """,(rs,row) -> new DailySales(rs.getDate("day").toLocalDate(),rs.getBigDecimal("total"),rs.getLong("count")),
                branchId,currency,start,end);
        return new Overview(currency,sales,expenses,saleCount,purchaseCount,employeeCount,lowStock,daily);
    }
    private void validateDates(LocalDate from,LocalDate to) {
        if (from==null || to==null || to.isBefore(from) || from.plusYears(2).isBefore(to))
            throw new IllegalArgumentException("Date range must be ordered and at most two years");
    }
}
