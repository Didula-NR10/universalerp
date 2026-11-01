package com.erp.modules.notifications;

import java.math.BigDecimal;
import java.util.UUID;

public interface NotificationPublisher {
    void lowStock(UUID branchId,UUID warehouseId,UUID productId,String productName,BigDecimal quantity,UUID movementId);
}
