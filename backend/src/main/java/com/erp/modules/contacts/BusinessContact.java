package com.erp.modules.contacts;

import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity @Table(name="business_contact")
class BusinessContact {
    @Id UUID id;
    @Column(name="branch_id",nullable=false) UUID branchId;
    @Column(name="contact_type",nullable=false) String contactType;
    @Column(nullable=false) String name;
    String email;
    String phone;
    @Column(nullable=false) String status;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @Version long version;
    protected BusinessContact() {}
    BusinessContact(UUID branchId,String type,String name,String email,String phone) {
        this.id=UUID.randomUUID(); this.branchId=branchId; this.contactType=type;
        this.name=name.trim(); this.email=email; this.phone=phone; this.status="ACTIVE";
        this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
}
