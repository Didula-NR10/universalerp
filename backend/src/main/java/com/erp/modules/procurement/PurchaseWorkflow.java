package com.erp.modules.procurement;

public final class PurchaseWorkflow {
    private PurchaseWorkflow() {}
    public static String submit(String status) {
        if (!"DRAFT".equals(status)) throw new IllegalArgumentException("Only draft orders can be submitted");
        return "SUBMITTED";
    }
    public static String approve(String status) {
        if (!"SUBMITTED".equals(status)) throw new IllegalArgumentException("Only submitted orders can be approved");
        return "APPROVED";
    }
    public static String reject(String status) {
        if (!"SUBMITTED".equals(status)) throw new IllegalArgumentException("Only submitted orders can be rejected");
        return "REJECTED";
    }
    public static String cancel(String status) {
        if (!"DRAFT".equals(status) && !"SUBMITTED".equals(status))
            throw new IllegalArgumentException("Only unapproved orders can be cancelled");
        return "CANCELLED";
    }
    public static String receive(String status, boolean allReceived) {
        if (!status.equals("APPROVED") && !status.equals("PARTIALLY_RECEIVED"))
            throw new IllegalArgumentException("Order is not approved for receiving");
        return allReceived ? "RECEIVED" : "PARTIALLY_RECEIVED";
    }
}
