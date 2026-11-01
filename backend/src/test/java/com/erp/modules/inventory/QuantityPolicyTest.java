package com.erp.modules.inventory;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QuantityPolicyTest {
    @Test void issueCannotMakeStockNegative() {
        assertThrows(IllegalArgumentException.class,
                () -> QuantityPolicy.afterIssue(new BigDecimal("2.000"), new BigDecimal("2.001")));
    }

    @Test void issueUsesExactDecimalArithmetic() {
        assertEquals(new BigDecimal("0.100"),
                QuantityPolicy.afterIssue(new BigDecimal("0.300"), new BigDecimal("0.200")));
    }

    @Test void rejectsNonPositiveAndExcessPrecision() {
        assertThrows(IllegalArgumentException.class, () -> QuantityPolicy.positive(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> QuantityPolicy.positive(new BigDecimal("1.0001")));
    }
}
