package com.erp.modules.inventory;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface StockMovementRepository extends JpaRepository<StockMovement,UUID> {
    Page<StockMovement> findByWarehouseIdOrderByOccurredAtDesc(UUID warehouseId, Pageable pageable);
}
