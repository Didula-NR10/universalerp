package com.erp.modules.finance;

import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface ExpenseRepository extends JpaRepository<Expense,UUID> {
    Page<Expense> findByBranchIdOrderByCreatedAtDesc(UUID branchId,Pageable pageable);
    @Query("select coalesce(sum(e.amount),0) from Expense e where e.branchId=:branchId and e.status=:status and e.currency=:currency")
    BigDecimal approvedTotal(@Param("branchId") UUID branchId,@Param("currency") String currency,@Param("status") ExpenseStatus status);
}
