package com.erp.modules.contacts;

import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface BusinessContactRepository extends JpaRepository<BusinessContact,UUID> {
    Page<BusinessContact> findByBranchIdAndContactTypeAndNameContainingIgnoreCase(UUID branchId,String contactType,String name,Pageable pageable);
}
