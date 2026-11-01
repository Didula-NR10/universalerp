package com.erp.modules.inventory;

import java.util.Optional;
import java.util.UUID;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface StockBalanceRepository extends JpaRepository<StockBalance,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from StockBalance b where b.warehouseId = :warehouseId and b.productId = :productId")
    Optional<StockBalance> lock(@Param("warehouseId") UUID warehouseId, @Param("productId") UUID productId);
    Optional<StockBalance> findByWarehouseIdAndProductId(UUID warehouseId, UUID productId);
}
