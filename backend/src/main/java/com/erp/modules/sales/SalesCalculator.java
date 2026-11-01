package com.erp.modules.sales;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class SalesCalculator {
    private SalesCalculator() {}
    public record Line(BigDecimal subtotal,BigDecimal discount,BigDecimal tax,BigDecimal total) {}
    public static Line calculate(BigDecimal quantity,BigDecimal unitPrice,BigDecimal discountRate,BigDecimal taxRate) {
        if (quantity==null || quantity.signum()<=0 || quantity.scale()>3 || quantity.precision()>19)
            throw new IllegalArgumentException("Invalid sale quantity");
        if (unitPrice==null || unitPrice.signum()<0 || unitPrice.scale()>2 || unitPrice.precision()>19)
            throw new IllegalArgumentException("Invalid unit price");
        validRate(discountRate); validRate(taxRate);
        BigDecimal subtotal=unitPrice.multiply(quantity).setScale(2,RoundingMode.HALF_UP);
        BigDecimal discount=subtotal.multiply(discountRate).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        BigDecimal taxable=subtotal.subtract(discount);
        BigDecimal tax=taxable.multiply(taxRate).divide(new BigDecimal("100"),2,RoundingMode.HALF_UP);
        return new Line(subtotal,discount,tax,taxable.add(tax));
    }
    private static void validRate(BigDecimal rate) {
        if (rate==null || rate.signum()<0 || rate.compareTo(new BigDecimal("100"))>0 || rate.scale()>2)
            throw new IllegalArgumentException("Invalid percentage rate");
    }
}
