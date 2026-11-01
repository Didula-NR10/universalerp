package com.erp.modules.employees;

import java.time.LocalDate;

public final class LeavePolicy {
    private LeavePolicy() {}
    public static void validDates(LocalDate start,LocalDate end) {
        if (start==null || end==null || end.isBefore(start)) throw new IllegalArgumentException("Invalid leave dates");
    }
    public static String decide(String status,boolean approve) {
        if (!"SUBMITTED".equals(status)) throw new IllegalArgumentException("Only submitted leave can be decided");
        return approve ? "APPROVED" : "REJECTED";
    }
}
