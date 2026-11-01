package com.erp.modules.finance;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name="expense")
class Expense {
    @Id UUID id;
    @Column(name="branch_id",nullable=false) UUID branchId;
    @Column(name="category_id",nullable=false) UUID categoryId;
    @Column(nullable=false) String description;
    @Column(nullable=false,precision=19,scale=2) BigDecimal amount;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,length=3,columnDefinition="char(3)") String currency;
    @Enumerated(EnumType.STRING) @Column(nullable=false) ExpenseStatus status;
    @Column(name="created_by",nullable=false) UUID createdBy;
    @Column(name="approved_by") UUID approvedBy;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @Version long version;
    protected Expense() {}
    Expense(UUID branchId,UUID categoryId,String description,BigDecimal amount,String currency,UUID createdBy) {
        this.id=UUID.randomUUID(); this.branchId=branchId; this.categoryId=categoryId; this.description=description.trim();
        this.amount=amount; this.currency=currency; this.createdBy=createdBy; this.status=ExpenseStatus.DRAFT;
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
}
