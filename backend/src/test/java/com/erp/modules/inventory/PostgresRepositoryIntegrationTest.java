package com.erp.modules.inventory;

import java.math.BigDecimal;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named="TEST_DB_DISPOSABLE",matches="true")
class PostgresRepositoryIntegrationTest {
    @Autowired Flyway flyway;
    @Autowired ProductRepository products;

    @Test @Transactional
    void migrationsAndProductRepositoryWorkOnPostgres() {
        assertTrue(flyway.info().applied().length >= 13,"Expected ERP migrations V1–V13");
        Product product=new Product("INTEGRATION-"+java.util.UUID.randomUUID(),null,"Integration product",
                new BigDecimal("12.50"),"USD",new BigDecimal("2.000"),BigDecimal.ZERO);
        products.saveAndFlush(product);
        assertEquals(product.id,products.findById(product.id).orElseThrow().id);
    }
}
