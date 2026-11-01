package com.erp.modules.employees;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeavePolicyTest {
    @Test void rejectsReverseDates() {
        assertThrows(IllegalArgumentException.class,() -> LeavePolicy.validDates(LocalDate.of(2026,2,2),LocalDate.of(2026,2,1)));
    }
    @Test void decidesOnce() {
        assertEquals("APPROVED",LeavePolicy.decide("SUBMITTED",true));
        assertThrows(IllegalArgumentException.class,() -> LeavePolicy.decide("APPROVED",false));
    }
}
