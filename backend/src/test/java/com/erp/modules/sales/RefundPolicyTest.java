package com.erp.modules.sales;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RefundPolicyTest {
    private BigDecimal d(String value) { return new BigDecimal(value); }

    @Test void lastPartialRefundReceivesRemainingRoundedAmount() {
        assertEquals(d("3.33"),RefundPolicy.amount(d("3"),d("10.00"),d("0"),d("0"),d("1")));
        assertEquals(d("3.34"),RefundPolicy.amount(d("3"),d("10.00"),d("2"),d("6.66"),d("1")));
    }
    @Test void cannotRefundMoreThanRemainingQuantity() {
        assertThrows(IllegalArgumentException.class,()->RefundPolicy.amount(d("2"),d("5.00"),d("1"),d("2.50"),d("2")));
        assertThrows(IllegalArgumentException.class,()->RefundPolicy.amount(d("2"),d("5.00"),d("0"),d("0"),d("0")));
    }
}
