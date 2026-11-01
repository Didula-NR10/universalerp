package com.erp.modules.finance;

import java.math.BigDecimal;
import java.util.List;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/finance")
class ExpenseController {
    private final ExpenseService service;
    ExpenseController(ExpenseService service) { this.service=service; }
    record CategoryRequest(@NotBlank @Size(max=100) String name) {}
    record CategoryView(UUID id,String name,String status) {
        static CategoryView of(ExpenseCategory c) { return new CategoryView(c.id,c.name,c.status); }
    }
    record ExpenseRequest(@NotNull UUID categoryId,@NotBlank @Size(max=500) String description,
                          @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
                          @NotBlank @Pattern(regexp="[A-Z]{3}") String currency) {}
    record ExpenseView(UUID id,UUID branchId,UUID categoryId,String description,BigDecimal amount,
                       String currency,ExpenseStatus status,UUID createdBy,UUID approvedBy) {
        static ExpenseView of(Expense e) { return new ExpenseView(e.id,e.branchId,e.categoryId,e.description,e.amount,e.currency,e.status,e.createdBy,e.approvedBy); }
    }
    record TotalView(String currency,BigDecimal approvedTotal) {}

    @GetMapping("/categories")
    List<CategoryView> categories(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId) {
        return service.categories(jwt,branchId).stream().map(CategoryView::of).toList();
    }
    @PostMapping("/categories") @ResponseStatus(HttpStatus.CREATED)
    CategoryView createCategory(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CategoryRequest request) {
        return CategoryView.of(service.createCategory(jwt,request.name()));
    }
    @GetMapping("/expenses")
    Page<ExpenseView> expenses(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                               @RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="25") int size) {
        return service.list(jwt,branchId,PageRequest.of(page,Math.min(Math.max(size,1),100))).map(ExpenseView::of);
    }
    @PostMapping("/expenses") @ResponseStatus(HttpStatus.CREATED)
    ExpenseView create(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@Valid @RequestBody ExpenseRequest request) {
        return ExpenseView.of(service.create(jwt,branchId,request.categoryId(),request.description(),request.amount(),request.currency()));
    }
    @PostMapping("/expenses/{id}/submit")
    ExpenseView submit(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return ExpenseView.of(service.submit(jwt,branchId,id));
    }
    @PostMapping("/expenses/{id}/approve")
    ExpenseView approve(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return ExpenseView.of(service.decide(jwt,branchId,id,true));
    }
    @PostMapping("/expenses/{id}/reject")
    ExpenseView reject(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return ExpenseView.of(service.decide(jwt,branchId,id,false));
    }
    @GetMapping("/summary")
    TotalView summary(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@RequestParam String currency) {
        return new TotalView(currency,service.approvedTotal(jwt,branchId,currency));
    }
}
