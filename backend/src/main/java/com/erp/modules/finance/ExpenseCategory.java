package com.erp.modules.finance;

import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity @Table(name="expense_category")
class ExpenseCategory {
    @Id UUID id;
    @Column(nullable=false) String name;
    @Column(nullable=false) String status;
    protected ExpenseCategory() {}
    ExpenseCategory(String name) { this.id=UUID.randomUUID(); this.name=name.trim(); this.status="ACTIVE"; }
}
