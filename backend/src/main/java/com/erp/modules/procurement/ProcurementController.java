package com.erp.modules.procurement;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.context.annotation.Profile;
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

@RestController @Profile("!foundation") @RequestMapping("/api/v1/purchases")
class ProcurementController {
    private final ProcurementService service;
    ProcurementController(ProcurementService service) { this.service=service; }
    record ItemRequest(@NotNull UUID productId,@NotNull @DecimalMin("0.001") @Digits(integer=16,fraction=3) BigDecimal quantity,
                       @NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal unitCost) {}
    record CreateRequest(@NotNull UUID supplierId,@NotNull UUID warehouseId,
                         @NotNull @Pattern(regexp="[A-Z]{3}") String currency,
                         @NotEmpty List<@Valid ItemRequest> items) {}
    record ReceiveItemRequest(@NotNull UUID orderItemId,@NotNull @DecimalMin("0.001") @Digits(integer=16,fraction=3) BigDecimal quantity) {}
    record ReceiveRequest(@NotEmpty List<@Valid ReceiveItemRequest> items) {}

    @GetMapping
    List<ProcurementService.OrderView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                            @RequestParam(defaultValue="25") int limit,@RequestParam(defaultValue="0") int offset) {
        return service.list(jwt,branchId,Math.min(Math.max(limit,1),100),Math.max(offset,0));
    }
    @GetMapping("/{id}/items")
    List<ProcurementService.ItemView> items(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.items(jwt,branchId,id);
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    ProcurementService.OrderView create(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@Valid @RequestBody CreateRequest request) {
        return service.create(jwt,branchId,request.supplierId(),request.warehouseId(),request.currency(),
                request.items().stream().map(i -> new ProcurementService.NewItem(i.productId(),i.quantity(),i.unitCost())).toList());
    }
    @PostMapping("/{id}/submit")
    ProcurementService.OrderView submit(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.submit(jwt,branchId,id);
    }
    @PostMapping("/{id}/approve")
    ProcurementService.OrderView approve(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.approve(jwt,branchId,id);
    }
    @PostMapping("/{id}/reject")
    ProcurementService.OrderView reject(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.reject(jwt,branchId,id);
    }
    @PostMapping("/{id}/cancel")
    ProcurementService.OrderView cancel(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.cancel(jwt,branchId,id);
    }
    @PostMapping("/{id}/receipts") @ResponseStatus(HttpStatus.CREATED)
    ProcurementService.ReceiptView receive(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,
                                           @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody ReceiveRequest request) {
        return service.receive(jwt,branchId,id,key,request.items().stream()
                .map(i -> new ProcurementService.ReceiveItem(i.orderItemId(),i.quantity())).toList());
    }
}
