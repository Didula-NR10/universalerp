package com.erp.modules.reporting;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class DashboardService {
    record Activity(String action,String resourceType,Instant occurredAt) {}
    record DailyRevenue(LocalDate day,BigDecimal total) {}
    record Dashboard(String currency,UUID branchId,LocalDate from,LocalDate to,
                     BigDecimal salesTotal,BigDecimal expenseTotal,long saleCount,long purchaseCount,
                     long employeeCount,long lowStockProducts,long pendingPurchases,long pendingExpenses,
                     long pendingLeaves,List<DailyRevenue> revenueTrend,List<Activity> recentActivity) {}

    private final NamedParameterJdbcTemplate jdbc;
    private final AccessService access;
    DashboardService(NamedParameterJdbcTemplate jdbc,AccessService access) { this.jdbc=jdbc;this.access=access; }

    @Transactional(readOnly=true)
    Dashboard load(Jwt jwt,UUID branchId) {
        access.require(jwt,"REPORT_VIEW",branchId);
        List<String> currencies=jdbc.query("SELECT base_currency FROM company_profile WHERE id=1",Map.of(),
                (rs,row)->rs.getString(1).trim());
        if (currencies.isEmpty()) throw new EntityNotFoundException("Configure the company profile to see financial metrics");
        String currency=currencies.get(0);
        LocalDate to=LocalDate.now(ZoneOffset.UTC);
        LocalDate from=to.minusDays(29);
        Timestamp start=Timestamp.from(from.atStartOfDay().toInstant(ZoneOffset.UTC));
        Timestamp end=Timestamp.from(to.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC));
        Map<String,Object> params=branchId==null
                ? Map.of("currency",currency,"start",start,"end",end)
                : Map.of("currency",currency,"start",start,"end",end,"branchId",branchId);
        String saleBranch=branchId==null?"":" AND s.branch_id=:branchId";
        String expenseBranch=branchId==null?"":" AND e.branch_id=:branchId";
        String purchaseBranch=branchId==null?"":" AND p.branch_id=:branchId";
        String employeeBranch=branchId==null?"":" AND e.branch_id=:branchId";
        String stockBranch=branchId==null?"":" AND w.branch_id=:branchId";
        String leaveBranch=branchId==null?"":" AND l.branch_id=:branchId";
        String auditBranch=branchId==null?"":" AND a.branch_id=:branchId";

        BigDecimal sales=money("SELECT coalesce(sum(s.total-coalesce((SELECT sum(r.amount) FROM sale_refund r WHERE r.sale_id=s.id AND r.status='COMPLETED'),0)),0) FROM sale s WHERE s.currency=:currency AND s.completed_at>=:start AND s.completed_at<:end"+saleBranch,params);
        BigDecimal expenses=money("SELECT coalesce(sum(e.amount),0) FROM expense e WHERE e.currency=:currency AND e.status='APPROVED' AND e.created_at>=:start AND e.created_at<:end"+expenseBranch,params);
        long saleCount=count("SELECT count(*) FROM sale s WHERE s.completed_at>=:start AND s.completed_at<:end"+saleBranch,params);
        long purchaseCount=count("SELECT count(*) FROM purchase_order p WHERE p.status NOT IN ('CANCELLED','REJECTED') AND p.created_at>=:start AND p.created_at<:end"+purchaseBranch,params);
        long employeeCount=count("SELECT count(*) FROM employee e WHERE e.status='ACTIVE'"+employeeBranch,params);
        long lowStock=count("SELECT count(*) FROM stock_balance b JOIN warehouse w ON w.id=b.warehouse_id JOIN product p ON p.id=b.product_id WHERE p.status='ACTIVE' AND b.quantity<p.reorder_level"+stockBranch,params);
        long pendingPurchases=count("SELECT count(*) FROM purchase_order p WHERE p.status='SUBMITTED'"+purchaseBranch,params);
        long pendingExpenses=count("SELECT count(*) FROM expense e WHERE e.status='SUBMITTED'"+expenseBranch,params);
        long pendingLeaves=count("SELECT count(*) FROM leave_request l WHERE l.status='SUBMITTED'"+leaveBranch,params);
        List<DailyRevenue> trend=jdbc.query("""
                SELECT (s.completed_at AT TIME ZONE 'UTC')::date AS day,
                  coalesce(sum(s.total-coalesce((SELECT sum(r.amount) FROM sale_refund r
                      WHERE r.sale_id=s.id AND r.status='COMPLETED'),0)),0) AS total
                FROM sale s WHERE s.currency=:currency
                AND s.completed_at>=:start AND s.completed_at<:end
                """+saleBranch+" GROUP BY day ORDER BY day",params,
                (rs,row)->new DailyRevenue(rs.getDate("day").toLocalDate(),rs.getBigDecimal("total")));
        List<Activity> recent=jdbc.query("SELECT a.action,a.resource_type,a.occurred_at FROM audit_log a WHERE a.outcome='SUCCESS'"+auditBranch+" ORDER BY a.occurred_at DESC LIMIT 8",
                params,(rs,row)->new Activity(rs.getString(1),rs.getString(2),rs.getTimestamp(3).toInstant()));
        return new Dashboard(currency,branchId,from,to,sales,expenses,saleCount,purchaseCount,
                employeeCount,lowStock,pendingPurchases,pendingExpenses,pendingLeaves,trend,recent);
    }

    private BigDecimal money(String sql,Map<String,Object> params) {
        BigDecimal value=jdbc.queryForObject(sql,params,BigDecimal.class);
        return value==null?BigDecimal.ZERO:value;
    }
    private long count(String sql,Map<String,Object> params) {
        Long value=jdbc.queryForObject(sql,params,Long.class);
        return value==null?0:value;
    }
}
