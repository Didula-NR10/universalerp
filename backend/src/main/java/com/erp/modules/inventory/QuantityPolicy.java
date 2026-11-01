package com.erp.modules.inventory;

import java.math.BigDecimal;

public final class QuantityPolicy {
    private QuantityPolicy() {}
    public static BigDecimal positive(BigDecimal quantity) {
        if (quantity == null || quantity.signum() <= 0 || quantity.scale() > 3 || quantity.precision() > 19)
            throw new IllegalArgumentException("Quantity must be positive with at most three decimal places");
        return quantity;
    }
    public static BigDecimal afterIssue(BigDecimal available, BigDecimal quantity) {
        positive(quantity);
        if (available.compareTo(quantity) < 0) throw new IllegalArgumentException("Insufficient stock");
        return available.subtract(quantity);
    }
}
