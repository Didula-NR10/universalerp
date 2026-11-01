package com.erp.modules.organization;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface WarehouseRepository extends JpaRepository<Warehouse, UUID> {
    List<Warehouse> findByBranchIdOrderByName(UUID branchId);
    boolean existsByCodeIgnoreCase(String code);
}
