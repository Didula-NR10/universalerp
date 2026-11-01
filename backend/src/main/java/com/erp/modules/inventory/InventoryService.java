package com.erp.modules.inventory;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.util.Map;
import java.util.UUID;
import com.erp.modules.organization.OrganizationDirectory;
import com.erp.modules.notifications.NotificationPublisher;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class InventoryService implements StockLedger {
    record StockResult(UUID movementId, BigDecimal quantity) {}
    private final ProductRepository products;
    private final StockBalanceRepository balances;
    private final StockMovementRepository movements;
    private final OrganizationDirectory organization;
    private final AccessService access;
    private final JdbcTemplate jdbc;
    private final NotificationPublisher notifications;

    InventoryService(ProductRepository products, StockBalanceRepository balances, StockMovementRepository movements,
                     OrganizationDirectory organization, AccessService access, JdbcTemplate jdbc, NotificationPublisher notifications) {
        this.products=products; this.balances=balances; this.movements=movements;
        this.organization=organization; this.access=access; this.jdbc=jdbc; this.notifications=notifications;
    }

    @Transactional(readOnly=true)
    Page<Product> products(Jwt jwt, UUID branchId, String search, Pageable pageable) {
        access.require(jwt, "PRODUCT_READ", branchId);
        if (search == null || search.isBlank()) return products.findAll(pageable);
        return products.findByNameContainingIgnoreCaseOrSkuContainingIgnoreCaseOrBarcodeContainingIgnoreCase(search, search, search, pageable);
    }

    @Transactional
    Product createProduct(Jwt jwt, UUID branchId, String sku, String barcode, String name,
                          BigDecimal price, String currency, BigDecimal reorderLevel, BigDecimal taxRate) {
        access.require(jwt, "PRODUCT_CREATE", branchId);
        if (products.existsBySkuIgnoreCase(sku)) throw new IllegalArgumentException("SKU already exists");
        if (price == null || price.signum() < 0 || price.scale() > 2 || price.precision() > 19)
            throw new IllegalArgumentException("Invalid product price");
        if (reorderLevel == null || reorderLevel.signum() < 0 || reorderLevel.scale() > 3)
            throw new IllegalArgumentException("Invalid reorder level");
        if (taxRate == null || taxRate.signum() < 0 || taxRate.compareTo(new BigDecimal("100")) > 0 || taxRate.scale() > 2)
            throw new IllegalArgumentException("Invalid tax rate");
        Product product = products.save(new Product(sku, barcode, name, price, currency, reorderLevel, taxRate));
        audit(jwt, branchId, "PRODUCT_CREATED", "PRODUCT", product.id.toString());
        return product;
    }

    @Transactional(readOnly=true)
    BigDecimal balance(Jwt jwt, UUID warehouseId, UUID productId) {
        access.require(jwt, "INVENTORY_VIEW", organization.branchForWarehouse(warehouseId));
        return balances.findByWarehouseIdAndProductId(warehouseId, productId)
                .map(balance -> balance.quantity).orElse(BigDecimal.ZERO);
    }

    @Transactional(readOnly=true)
    Page<StockMovement> movements(Jwt jwt, UUID warehouseId, Pageable pageable) {
        access.require(jwt, "INVENTORY_VIEW", organization.branchForWarehouse(warehouseId));
        return movements.findByWarehouseIdOrderByOccurredAtDesc(warehouseId, pageable);
    }

    @Transactional
    StockResult adjust(Jwt jwt, UUID warehouseId, UUID productId, BigDecimal quantity,
                       String type, UUID idempotencyKey) {
        UUID branchId = organization.branchForWarehouse(warehouseId);
        access.require(jwt, "INVENTORY_ADJUST", branchId);
        QuantityPolicy.positive(quantity);
        if (!type.equals("RECEIPT") && !type.equals("ISSUE")) throw new IllegalArgumentException("Unsupported movement type");
        StockResult prior = reserve(idempotencyKey, type);
        if (prior != null) return prior;
        Product product=activeProduct(productId);
        StockBalance balance = lockedBalance(warehouseId, productId);
        BigDecimal before=balance.quantity;
        BigDecimal delta = type.equals("RECEIPT") ? quantity : quantity.negate();
        balance.quantity = type.equals("RECEIPT") ? balance.quantity.add(quantity) : QuantityPolicy.afterIssue(balance.quantity, quantity);
        StockMovement movement = movements.save(new StockMovement(warehouseId, productId, delta, type, idempotencyKey.toString(), access.userId(jwt)));
        lowStock(branchId,warehouseId,product,before,balance.quantity,movement.id);
        complete(idempotencyKey, movement.id, balance.quantity);
        audit(jwt, branchId, "STOCK_" + type, "STOCK_MOVEMENT", movement.id.toString());
        return new StockResult(movement.id, balance.quantity);
    }

    @Transactional
    StockResult transfer(Jwt jwt, UUID sourceId, UUID destinationId, UUID productId, BigDecimal quantity, UUID idempotencyKey) {
        if (sourceId.equals(destinationId)) throw new IllegalArgumentException("Source and destination must differ");
        UUID sourceBranch = organization.branchForWarehouse(sourceId);
        UUID destinationBranch = organization.branchForWarehouse(destinationId);
        access.require(jwt, "INVENTORY_TRANSFER", sourceBranch);
        access.require(jwt, "INVENTORY_TRANSFER", destinationBranch);
        QuantityPolicy.positive(quantity);
        StockResult prior = reserve(idempotencyKey, "TRANSFER");
        if (prior != null) return prior;
        Product product=activeProduct(productId);
        // Lock both balances in a stable order to avoid transfer deadlocks.
        UUID first = sourceId.compareTo(destinationId) < 0 ? sourceId : destinationId;
        UUID second = first.equals(sourceId) ? destinationId : sourceId;
        StockBalance firstBalance = lockedBalance(first, productId);
        StockBalance secondBalance = lockedBalance(second, productId);
        StockBalance source = sourceId.equals(first) ? firstBalance : secondBalance;
        StockBalance destination = destinationId.equals(first) ? firstBalance : secondBalance;
        BigDecimal sourceBefore=source.quantity;
        source.quantity = QuantityPolicy.afterIssue(source.quantity, quantity);
        destination.quantity = destination.quantity.add(quantity);
        StockMovement out = movements.save(new StockMovement(sourceId, productId, quantity.negate(), "TRANSFER_OUT", idempotencyKey.toString(), access.userId(jwt)));
        lowStock(sourceBranch,sourceId,product,sourceBefore,source.quantity,out.id);
        movements.save(new StockMovement(destinationId, productId, quantity, "TRANSFER_IN", idempotencyKey.toString(), access.userId(jwt)));
        complete(idempotencyKey, out.id, source.quantity);
        audit(jwt, sourceBranch, "STOCK_TRANSFER", "STOCK_MOVEMENT", out.id.toString());
        return new StockResult(out.id, source.quantity);
    }

    @Override @Transactional
    public void receivePurchase(Jwt actor, UUID warehouseId, UUID productId, BigDecimal quantity, UUID receiptLineId) {
        linkedChange(actor,warehouseId,productId,quantity,receiptLineId,"PURCHASE_RECEIVE","PURCHASE_RECEIPT",false);
    }

    @Override @Transactional
    public void issueSale(Jwt actor, UUID warehouseId, UUID productId, BigDecimal quantity, UUID saleLineId) {
        linkedChange(actor,warehouseId,productId,quantity,saleLineId,"SALE_CREATE","SALE_ISSUE",true);
    }

    @Override @Transactional
    public void receiveReturn(Jwt actor, UUID warehouseId, UUID productId, BigDecimal quantity, UUID refundLineId) {
        linkedChange(actor,warehouseId,productId,quantity,refundLineId,"SALE_REFUND","SALE_REFUND",false);
    }

    private void linkedChange(Jwt jwt,UUID warehouseId,UUID productId,BigDecimal quantity,UUID key,
                              String permission,String operationType,boolean issue) {
        UUID branchId=organization.branchForWarehouse(warehouseId);
        access.require(jwt,permission,branchId);
        QuantityPolicy.positive(quantity);
        if (reserve(key,operationType)!=null) return;
        Product product=activeProduct(productId);
        StockBalance balance=lockedBalance(warehouseId,productId);
        BigDecimal before=balance.quantity;
        balance.quantity=issue ? QuantityPolicy.afterIssue(balance.quantity,quantity) : balance.quantity.add(quantity);
        StockMovement movement=movements.save(new StockMovement(warehouseId,productId,
                issue ? quantity.negate() : quantity,issue ? "ISSUE" : "RECEIPT",
                operationType.equals("SALE_REFUND") ? "SALE_REFUND" : issue ? "SALE" : "PURCHASE_RECEIPT",
                key.toString(),access.userId(jwt)));
        lowStock(branchId,warehouseId,product,before,balance.quantity,movement.id);
        complete(key,movement.id,balance.quantity);
        audit(jwt,branchId,issue ? "SALE_STOCK_ISSUED" : "PURCHASE_STOCK_RECEIVED","STOCK_MOVEMENT",movement.id.toString());
    }

    private Product activeProduct(UUID productId) {
        Product product = products.findById(productId).orElseThrow(() -> new EntityNotFoundException("Product not found"));
        if (!"ACTIVE".equals(product.status)) throw new IllegalArgumentException("Product is inactive");
        return product;
    }

    private void lowStock(UUID branchId,UUID warehouseId,Product product,BigDecimal before,BigDecimal after,UUID movementId) {
        if (before.compareTo(product.reorderLevel)>=0 && after.compareTo(product.reorderLevel)<0)
            notifications.lowStock(branchId,warehouseId,product.id,product.name,after,movementId);
    }

    private StockBalance lockedBalance(UUID warehouseId, UUID productId) {
        jdbc.update("INSERT INTO stock_balance(id,warehouse_id,product_id,quantity,version) VALUES (?,?,?,?,0) ON CONFLICT (warehouse_id,product_id) DO NOTHING",
                UUID.randomUUID(), warehouseId, productId, BigDecimal.ZERO);
        return balances.lock(warehouseId, productId).orElseThrow();
    }

    private StockResult reserve(UUID key, String type) {
        int inserted = jdbc.update("INSERT INTO stock_operation(idempotency_key,operation_type) VALUES (?,?) ON CONFLICT DO NOTHING", key, type);
        if (inserted == 1) return null;
        return jdbc.queryForObject("SELECT movement_id,result_quantity,operation_type FROM stock_operation WHERE idempotency_key=?", (ResultSet rs, int row) -> {
            if (!type.equals(rs.getString("operation_type"))) throw new IllegalArgumentException("Idempotency key used for another operation");
            return new StockResult(rs.getObject("movement_id", UUID.class), rs.getBigDecimal("result_quantity"));
        }, key);
    }

    private void complete(UUID key, UUID movementId, BigDecimal quantity) {
        jdbc.update("UPDATE stock_operation SET movement_id=?, result_quantity=? WHERE idempotency_key=?", movementId, quantity, key);
    }

    private void audit(Jwt jwt, UUID branchId, String action, String resourceType, String resourceId) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,?,?,'SUCCESS')",
                access.userId(jwt), branchId, action, resourceType, resourceId);
    }
}
