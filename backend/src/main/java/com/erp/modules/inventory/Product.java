package com.erp.modules.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "product")
class Product {
    @Id UUID id;
    @Column(name="category_id") UUID categoryId;
    @Column(nullable=false) String sku;
    String barcode;
    @Column(nullable=false) String name;
    String description;
    @Column(nullable=false) String unit;
    @Column(nullable=false, precision=19, scale=2) BigDecimal price;
    @JdbcTypeCode(SqlTypes.CHAR) @Column(nullable=false,length=3,columnDefinition="char(3)") String currency;
    @Column(name="reorder_level", nullable=false, precision=19, scale=3) BigDecimal reorderLevel;
    @Column(name="tax_rate", nullable=false, precision=5, scale=2) BigDecimal taxRate;
    @Column(nullable=false) String status;
    @Column(name="created_at", nullable=false) Instant createdAt;
    @Column(name="updated_at", nullable=false) Instant updatedAt;
    @Version long version;

    protected Product() {}
    Product(String sku, String barcode, String name, BigDecimal price, String currency, BigDecimal reorderLevel, BigDecimal taxRate) {
        this.id=UUID.randomUUID(); this.sku=sku.trim().toUpperCase(); this.barcode=barcode;
        this.name=name.trim(); this.unit="EACH"; this.price=price; this.currency=currency.toUpperCase();
        this.reorderLevel=reorderLevel; this.taxRate=taxRate; this.status="ACTIVE"; this.createdAt=Instant.now(); this.updatedAt=this.createdAt;
    }
}
