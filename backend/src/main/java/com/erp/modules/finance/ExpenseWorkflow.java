package com.erp.modules.finance;

public final class ExpenseWorkflow {
    private ExpenseWorkflow() {}
    public static ExpenseStatus submit(ExpenseStatus current) {
        if (current != ExpenseStatus.DRAFT) throw new IllegalArgumentException("Only draft expenses can be submitted");
        return ExpenseStatus.SUBMITTED;
    }
    public static ExpenseStatus decide(ExpenseStatus current, boolean approve) {
        if (current != ExpenseStatus.SUBMITTED) throw new IllegalArgumentException("Only submitted expenses can be decided");
        return approve ? ExpenseStatus.APPROVED : ExpenseStatus.REJECTED;
    }
}
