package com.erp.modules.finance;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ExpenseWorkflowTest {
    @Test void validTransitions() {
        assertEquals(ExpenseStatus.SUBMITTED,ExpenseWorkflow.submit(ExpenseStatus.DRAFT));
        assertEquals(ExpenseStatus.APPROVED,ExpenseWorkflow.decide(ExpenseStatus.SUBMITTED,true));
        assertEquals(ExpenseStatus.REJECTED,ExpenseWorkflow.decide(ExpenseStatus.SUBMITTED,false));
    }
    @Test void invalidTransitions() {
        assertThrows(IllegalArgumentException.class,() -> ExpenseWorkflow.submit(ExpenseStatus.APPROVED));
        assertThrows(IllegalArgumentException.class,() -> ExpenseWorkflow.decide(ExpenseStatus.DRAFT,true));
    }
}
