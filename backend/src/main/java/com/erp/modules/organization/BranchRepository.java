package com.erp.modules.organization;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface BranchRepository extends JpaRepository<Branch, UUID> {
    boolean existsByCodeIgnoreCase(String code);
}
