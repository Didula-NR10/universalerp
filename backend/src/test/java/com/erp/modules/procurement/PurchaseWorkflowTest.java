package com.erp.modules.procurement;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PurchaseWorkflowTest {
    @Test void approvedOrderCanBePartiallyReceived() {
        assertEquals("SUBMITTED",PurchaseWorkflow.submit("DRAFT"));
        assertEquals("APPROVED",PurchaseWorkflow.approve("SUBMITTED"));
        assertEquals("PARTIALLY_RECEIVED",PurchaseWorkflow.receive("APPROVED",false));
        assertEquals("RECEIVED",PurchaseWorkflow.receive("PARTIALLY_RECEIVED",true));
    }
    @Test void invalidTransitionsFail() {
        assertThrows(IllegalArgumentException.class,() -> PurchaseWorkflow.approve("DRAFT"));
        assertThrows(IllegalArgumentException.class,() -> PurchaseWorkflow.receive("SUBMITTED",false));
        assertEquals("REJECTED",PurchaseWorkflow.reject("SUBMITTED"));
        assertEquals("CANCELLED",PurchaseWorkflow.cancel("DRAFT"));
        assertEquals("CANCELLED",PurchaseWorkflow.cancel("SUBMITTED"));
        assertThrows(IllegalArgumentException.class,() -> PurchaseWorkflow.reject("APPROVED"));
        assertThrows(IllegalArgumentException.class,() -> PurchaseWorkflow.cancel("PARTIALLY_RECEIVED"));
    }
}
