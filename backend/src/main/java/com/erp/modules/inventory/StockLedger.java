package com.erp.modules.inventory;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

public interface StockLedger {
    void receivePurchase(Jwt actor, UUID warehouseId, UUID productId, BigDecimal quantity, UUID receiptLineId);
    void issueSale(Jwt actor, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleLineId);
    void receiveReturn(Jwt actor, UUID warehouseId, UUID productId, BigDecimal quantity, UUID refundLineId);
}
