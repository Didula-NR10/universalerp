package com.erp.modules.organization;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name = "branch")
class Branch {
    @Id UUID id;
    @Column(nullable = false) String code;
    @Column(nullable = false) String name;
    @Column(nullable = false) String status;
    String timezone;
    @Column(name = "created_at", nullable = false) Instant createdAt;
    @Column(name = "updated_at", nullable = false) Instant updatedAt;
    @Version long version;

    protected Branch() {}
    Branch(String code, String name, String timezone) {
        this.id = UUID.randomUUID(); this.code = code; this.name = name; this.timezone = timezone;
        this.status = "ACTIVE"; this.createdAt = Instant.now(); this.updatedAt = this.createdAt;
    }
}
