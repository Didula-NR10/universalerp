package com.erp.modules.sales;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SalesCalculatorTest {
    @Test void preservesExactHistoricalAmounts() {
        var line=SalesCalculator.calculate(new BigDecimal("2"),new BigDecimal("10.00"),
                new BigDecimal("10"),new BigDecimal("5"));
        assertEquals(new BigDecimal("20.00"),line.subtotal());
        assertEquals(new BigDecimal("2.00"),line.discount());
        assertEquals(new BigDecimal("0.90"),line.tax());
        assertEquals(new BigDecimal("18.90"),line.total());
    }
    @Test void rejectsInvalidRates() {
        assertThrows(IllegalArgumentException.class,() -> SalesCalculator.calculate(BigDecimal.ONE,BigDecimal.TEN,
                new BigDecimal("101"),BigDecimal.ZERO));
    }
}
