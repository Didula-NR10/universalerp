package com.erp.modules.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class RefundPolicy {
    private RefundPolicy() {}

    static BigDecimal amount(BigDecimal originalQuantity,BigDecimal originalAmount,
                             BigDecimal alreadyRequestedQuantity,BigDecimal alreadyRequestedAmount,
                             BigDecimal requestedQuantity) {
        if (requestedQuantity==null || requestedQuantity.signum()<=0 || requestedQuantity.scale()>3)
            throw new IllegalArgumentException("Invalid refund quantity");
        BigDecimal remainingQuantity=originalQuantity.subtract(alreadyRequestedQuantity);
        BigDecimal remainingAmount=originalAmount.subtract(alreadyRequestedAmount);
        if (remainingQuantity.signum()<=0 || requestedQuantity.compareTo(remainingQuantity)>0 || remainingAmount.signum()<0)
            throw new IllegalArgumentException("Refund exceeds remaining sale quantity or amount");
        if (requestedQuantity.compareTo(remainingQuantity)==0) return remainingAmount;
        BigDecimal proportional=originalAmount.multiply(requestedQuantity)
                .divide(originalQuantity,2,RoundingMode.HALF_UP);
        return proportional.min(remainingAmount);
    }
}
