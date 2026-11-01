package com.erp.modules.sales;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

@RestController @Profile("!foundation") @RequestMapping("/api/v1/sales")
class SalesController {
    private final SalesService service;
    private final PartialRefundService refunds;
    SalesController(SalesService service,PartialRefundService refunds) { this.service=service;this.refunds=refunds; }
    record ItemRequest(@NotNull UUID productId,@NotNull @DecimalMin("0.001") @Digits(integer=16,fraction=3) BigDecimal quantity,
                       @NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal discountRate) {}
    record CheckoutRequest(@NotNull UUID warehouseId,UUID customerId,@NotEmpty List<@Valid ItemRequest> items) {}
    record RefundItemRequest(@NotNull UUID saleItemId,@NotNull @DecimalMin("0.001") @Digits(integer=16,fraction=3) BigDecimal quantity,
                             boolean restock) {}
    record RefundRequest(@NotBlank @Size(max=500) String reason,@NotEmpty List<@Valid RefundItemRequest> items) {}

    @GetMapping
    List<SalesService.SaleView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                    @RequestParam(defaultValue="25") int limit,@RequestParam(defaultValue="0") int offset) {
        return service.list(jwt,branchId,Math.min(Math.max(limit,1),100),Math.max(offset,0));
    }
    @GetMapping("/{id}")
    SalesService.SaleView detail(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.detail(jwt,branchId,id);
    }
    @GetMapping("/{id}/items")
    List<SalesService.LineView> items(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.lines(jwt,branchId,id);
    }
    @PostMapping("/cash") @ResponseStatus(HttpStatus.CREATED)
    SalesService.SaleView cash(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                               @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody CheckoutRequest request) {
        return service.completeCashSale(jwt,branchId,request.warehouseId(),request.customerId(),key,
                request.items().stream().map(i -> new SalesService.NewLine(i.productId(),i.quantity(),i.discountRate())).toList());
    }
    @PostMapping("/{id}/refund")
    SalesService.SaleView refund(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,
                                 @RequestHeader("Idempotency-Key") UUID key) {
        return service.refundCashSale(jwt,branchId,id,key);
    }
    @GetMapping("/{id}/refunds")
    List<PartialRefundService.RefundView> refunds(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return refunds.list(jwt,branchId,id);
    }
    @GetMapping("/{id}/refunds/{refundId}/items")
    List<PartialRefundService.RefundLine> refundItems(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                                       @PathVariable UUID id,@PathVariable UUID refundId) {
        return refunds.lines(jwt,branchId,id,refundId);
    }
    @PostMapping("/{id}/refund-requests") @ResponseStatus(HttpStatus.CREATED)
    PartialRefundService.RefundView requestRefund(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,
                                                  @RequestHeader("Idempotency-Key") UUID key,@Valid @RequestBody RefundRequest request) {
        return refunds.request(jwt,branchId,id,key,request.reason(),request.items().stream()
                .map(item->new PartialRefundService.RequestedLine(item.saleItemId(),item.quantity(),item.restock())).toList());
    }
    @PostMapping("/{id}/refunds/{refundId}/approve")
    PartialRefundService.RefundView approveRefund(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                                   @PathVariable UUID id,@PathVariable UUID refundId) {
        return refunds.decide(jwt,branchId,id,refundId,true);
    }
    @PostMapping("/{id}/refunds/{refundId}/reject")
    PartialRefundService.RefundView rejectRefund(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                                  @PathVariable UUID id,@PathVariable UUID refundId) {
        return refunds.decide(jwt,branchId,id,refundId,false);
    }
}
