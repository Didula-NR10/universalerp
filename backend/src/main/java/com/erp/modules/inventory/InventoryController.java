package com.erp.modules.inventory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1")
class InventoryController {
    private final InventoryService service;
    InventoryController(InventoryService service) { this.service=service; }

    record ProductCreate(@NotBlank @Size(max=80) String sku, @Size(max=80) String barcode,
                         @NotBlank @Size(max=200) String name,
                         @NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal price,
                         @NotBlank @Pattern(regexp="[A-Z]{3}") String currency,
                         @NotNull @DecimalMin("0") @Digits(integer=16,fraction=3) BigDecimal reorderLevel,
                         @NotNull @DecimalMin("0") @Digits(integer=3,fraction=2) BigDecimal taxRate) {}
    record ProductView(UUID id, String sku, String barcode, String name, BigDecimal price, String currency,
                       BigDecimal reorderLevel, BigDecimal taxRate, String status) {
        static ProductView of(Product p) { return new ProductView(p.id,p.sku,p.barcode,p.name,p.price,p.currency,p.reorderLevel,p.taxRate,p.status); }
    }
    record StockChange(@NotNull UUID productId, @NotNull @DecimalMin("0.001") @Digits(integer=16,fraction=3) BigDecimal quantity) {}
    record Transfer(@NotNull UUID productId, @NotNull UUID destinationWarehouseId,
                    @NotNull @DecimalMin("0.001") @Digits(integer=16,fraction=3) BigDecimal quantity) {}
    record BalanceView(UUID warehouseId, UUID productId, BigDecimal quantity) {}
    record MovementView(UUID id, UUID warehouseId, UUID productId, BigDecimal quantityDelta,
                        String movementType, String referenceId, Instant occurredAt) {
        static MovementView of(StockMovement m) { return new MovementView(m.id,m.warehouseId,m.productId,m.quantityDelta,m.movementType,m.referenceId,m.occurredAt); }
    }

    @GetMapping("/products")
    Page<ProductView> products(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID branchId,
                               @RequestParam(defaultValue="") String search,
                               @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return service.products(jwt,branchId,search,PageRequest.of(page,Math.min(Math.max(size,1),100))).map(ProductView::of);
    }

    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    ProductView create(@AuthenticationPrincipal Jwt jwt, @RequestParam UUID branchId, @Valid @RequestBody ProductCreate request) {
        return ProductView.of(service.createProduct(jwt,branchId,request.sku(),request.barcode(),request.name(),request.price(),request.currency(),request.reorderLevel(),request.taxRate()));
    }

    @GetMapping("/warehouses/{warehouseId}/stock/{productId}")
    BalanceView balance(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID warehouseId, @PathVariable UUID productId) {
        return new BalanceView(warehouseId,productId,service.balance(jwt,warehouseId,productId));
    }

    @GetMapping("/warehouses/{warehouseId}/movements")
    Page<MovementView> movements(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID warehouseId,
                                 @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return service.movements(jwt,warehouseId,PageRequest.of(page,Math.min(Math.max(size,1),100))).map(MovementView::of);
    }

    @PostMapping("/warehouses/{warehouseId}/receipts") @ResponseStatus(HttpStatus.CREATED)
    InventoryService.StockResult receipt(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID warehouseId,
                                          @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody StockChange request) {
        return service.adjust(jwt,warehouseId,request.productId(),request.quantity(),"RECEIPT",key);
    }

    @PostMapping("/warehouses/{warehouseId}/issues") @ResponseStatus(HttpStatus.CREATED)
    InventoryService.StockResult issue(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID warehouseId,
                                        @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody StockChange request) {
        return service.adjust(jwt,warehouseId,request.productId(),request.quantity(),"ISSUE",key);
    }

    @PostMapping("/warehouses/{warehouseId}/transfers") @ResponseStatus(HttpStatus.CREATED)
    InventoryService.StockResult transfer(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID warehouseId,
                                           @RequestHeader("Idempotency-Key") UUID key, @Valid @RequestBody Transfer request) {
        return service.transfer(jwt,warehouseId,request.destinationWarehouseId(),request.productId(),request.quantity(),key);
    }
}
