package com.erp.modules.organization;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name="department")
class Department {
    @Id UUID id;
    @Column(name="branch_id") UUID branchId;
    @Column(nullable=false) String code;
    @Column(nullable=false) String name;
    @Column(nullable=false) String status;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @Version long version;
    protected Department() {}
    Department(UUID branchId,String code,String name) {
        this.id=UUID.randomUUID();this.branchId=branchId;this.code=code.trim().toUpperCase();
        this.name=name.trim();this.status="ACTIVE";this.createdAt=Instant.now();this.updatedAt=this.createdAt;
    }
}
