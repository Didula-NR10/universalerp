package com.erp.modules.inventory;

import java.math.BigDecimal;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name="stock_balance")
class StockBalance {
    @Id UUID id;
    @Column(name="warehouse_id", nullable=false) UUID warehouseId;
    @Column(name="product_id", nullable=false) UUID productId;
    @Column(nullable=false, precision=19, scale=3) BigDecimal quantity;
    @Version long version;
    protected StockBalance() {}
}
