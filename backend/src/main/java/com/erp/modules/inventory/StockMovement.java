package com.erp.modules.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name="stock_movement")
class StockMovement {
    @Id UUID id;
    @Column(name="warehouse_id", nullable=false) UUID warehouseId;
    @Column(name="product_id", nullable=false) UUID productId;
    @Column(name="quantity_delta", nullable=false, precision=19, scale=3) BigDecimal quantityDelta;
    @Column(name="movement_type", nullable=false) String movementType;
    @Column(name="reference_type", nullable=false) String referenceType;
    @Column(name="reference_id", nullable=false) String referenceId;
    @Column(name="actor_user_id", nullable=false) UUID actorUserId;
    @Column(name="occurred_at", nullable=false) Instant occurredAt;
    protected StockMovement() {}
    StockMovement(UUID warehouseId, UUID productId, BigDecimal delta, String type, String referenceId, UUID actorUserId) {
        this(warehouseId,productId,delta,type,"MANUAL",referenceId,actorUserId);
    }
    StockMovement(UUID warehouseId, UUID productId, BigDecimal delta, String type, String referenceType, String referenceId, UUID actorUserId) {
        this.id=UUID.randomUUID(); this.warehouseId=warehouseId; this.productId=productId;
        this.quantityDelta=delta; this.movementType=type; this.referenceType=referenceType;
        this.referenceId=referenceId; this.actorUserId=actorUserId; this.occurredAt=Instant.now();
    }
}
