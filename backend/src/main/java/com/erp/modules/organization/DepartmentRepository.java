package com.erp.modules.organization;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface DepartmentRepository extends JpaRepository<Department,UUID> {
    List<Department> findByBranchIdOrderByName(UUID branchId);
    boolean existsByBranchIdAndCodeIgnoreCase(UUID branchId,String code);
}
