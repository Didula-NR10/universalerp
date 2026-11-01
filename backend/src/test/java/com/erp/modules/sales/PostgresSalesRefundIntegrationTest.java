package com.erp.modules.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named="TEST_DB_DISPOSABLE",matches="true")
class PostgresSalesRefundIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager entityManager;
    @Autowired SalesService sales;
    @Autowired PartialRefundService refunds;

    @Test @Transactional
    void cashSaleAndTwoPartialRefundsPreserveStockAndIdempotency() {
        UUID branchId=UUID.randomUUID(),warehouseId=UUID.randomUUID(),productId=UUID.randomUUID();
        UUID requesterId=UUID.randomUUID(),approverId=UUID.randomUUID();
        String suffix=branchId.toString().substring(0,8);
        jdbc.update("INSERT INTO branch(id,code,name) VALUES (?,?,?)",branchId,"T"+suffix,"Test branch");
        jdbc.update("INSERT INTO warehouse(id,branch_id,code,name) VALUES (?,?,?,?)",warehouseId,branchId,"W"+suffix,"Test warehouse");
        jdbc.update("INSERT INTO product(id,sku,name,price,currency,reorder_level,tax_rate) VALUES (?,?,?,?,?,?,?)",
                productId,"P"+suffix,"Test product",new BigDecimal("5.00"),"USD",BigDecimal.ZERO,BigDecimal.ZERO);
        jdbc.update("INSERT INTO stock_balance(warehouse_id,product_id,quantity) VALUES (?,?,?)",
                warehouseId,productId,new BigDecimal("10.000"));
        addAdministrator(requesterId,"requester-"+suffix+"@example.invalid");
        addAdministrator(approverId,"approver-"+suffix+"@example.invalid");
        Jwt requester=jwt(requesterId),approver=jwt(approverId);

        UUID saleKey=UUID.randomUUID();
        List<SalesService.NewLine> items=List.of(new SalesService.NewLine(productId,new BigDecimal("2.000"),BigDecimal.ZERO));
        var sale=sales.completeCashSale(requester,branchId,warehouseId,null,saleKey,items);
        assertEquals("COMPLETED",sale.status());
        assertEquals(0,new BigDecimal("10.00").compareTo(sale.total()));
        assertEquals(sale.id(),sales.completeCashSale(requester,branchId,warehouseId,null,saleKey,items).id());
        assertStock(warehouseId,productId,"8.000");
        UUID lineId=sales.lines(requester,branchId,sale.id()).get(0).id();

        UUID firstKey=UUID.randomUUID();
        List<PartialRefundService.RequestedLine> firstLines=List.of(
                new PartialRefundService.RequestedLine(lineId,new BigDecimal("1.000"),true));
        var first=refunds.request(requester,branchId,sale.id(),firstKey,"Customer return",firstLines);
        assertEquals("PENDING_APPROVAL",first.status());
        assertEquals(0,new BigDecimal("5.00").compareTo(first.amount()));
        assertEquals(first.id(),refunds.request(requester,branchId,sale.id(),firstKey,"Customer return",firstLines).id());
        assertThrows(AccessDeniedException.class,()->refunds.decide(requester,branchId,sale.id(),first.id(),true));
        assertEquals("COMPLETED",refunds.decide(approver,branchId,sale.id(),first.id(),true).status());
        assertEquals("PARTIALLY_REFUNDED",sales.detail(requester,branchId,sale.id()).status());
        assertStock(warehouseId,productId,"9.000");

        var second=refunds.request(requester,branchId,sale.id(),UUID.randomUUID(),"Second return",firstLines);
        refunds.decide(approver,branchId,sale.id(),second.id(),true);
        assertEquals("REFUNDED",sales.detail(requester,branchId,sale.id()).status());
        assertStock(warehouseId,productId,"10.000");
        assertEquals(2,refunds.list(requester,branchId,sale.id()).size());
    }

    private void addAdministrator(UUID userId,String email) {
        jdbc.update("INSERT INTO user_profile(auth_user_id,email,display_name) VALUES (?,?,?)",userId,email,"Test admin");
        jdbc.update("INSERT INTO local_credential(user_id,password_hash,must_change_password) VALUES (?,'integration-test-only',false)",userId);
        jdbc.update("INSERT INTO user_role(user_id,role_id) SELECT ?,id FROM erp_role WHERE code='SUPER_ADMIN'",userId);
    }
    private Jwt jwt(UUID userId) {
        return Jwt.withTokenValue("integration-test").header("alg","none").subject(userId.toString()).build();
    }
    private void assertStock(UUID warehouseId,UUID productId,String expected) {
        entityManager.flush();
        BigDecimal quantity=jdbc.queryForObject("SELECT quantity FROM stock_balance WHERE warehouse_id=? AND product_id=?",
                BigDecimal.class,warehouseId,productId);
        assertNotNull(quantity);
        assertEquals(0,new BigDecimal(expected).compareTo(quantity));
    }
}
