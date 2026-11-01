package com.erp.modules.finance;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface ExpenseCategoryRepository extends JpaRepository<ExpenseCategory,UUID> {
    boolean existsByNameIgnoreCase(String name);
}
