package com.erp.modules.organization;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1")
class OrganizationController {
    private final OrganizationService service;
    OrganizationController(OrganizationService service) { this.service = service; }

    record CreateBranch(@NotBlank @Size(max=32) String code, @NotBlank @Size(max=160) String name, @Size(max=80) String timezone) {}
    record CompanyRequest(@NotBlank @Size(max=200) String legalName,@NotBlank @Size(max=200) String displayName,
                          @NotNull @Pattern(regexp="[A-Z]{3}") String baseCurrency,@NotBlank @Size(max=80) String timezone) {}
    record BranchUpdate(@NotBlank @Size(max=160) String name,@Size(max=80) String timezone,
                        @NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    record BranchView(UUID id, String code, String name, String status, String timezone) {
        static BranchView of(Branch b) { return new BranchView(b.id, b.code, b.name, b.status, b.timezone); }
    }
    record CreateWarehouse(@NotBlank @Size(max=32) String code, @NotBlank @Size(max=160) String name) {}
    record WarehouseUpdate(@NotBlank @Size(max=160) String name,@NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    record DepartmentRequest(@NotBlank @Size(max=32) String code,@NotBlank @Size(max=160) String name) {}
    record DepartmentUpdate(@NotBlank @Size(max=160) String name,@NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    record DepartmentView(UUID id,UUID branchId,String code,String name,String status) {
        static DepartmentView of(Department d) { return new DepartmentView(d.id,d.branchId,d.code,d.name,d.status); }
    }
    record WarehouseView(UUID id, UUID branchId, String code, String name, String status) {
        static WarehouseView of(Warehouse w) { return new WarehouseView(w.id, w.branchId, w.code, w.name, w.status); }
    }

    @GetMapping("/company")
    OrganizationService.CompanyView company(@AuthenticationPrincipal Jwt jwt) { return service.company(jwt); }

    @PutMapping("/company")
    OrganizationService.CompanyView company(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody CompanyRequest request) {
        return service.saveCompany(jwt,request.legalName(),request.displayName(),request.baseCurrency(),request.timezone());
    }

    @GetMapping("/branches")
    List<BranchView> branches(@AuthenticationPrincipal Jwt jwt) { return service.branches(jwt).stream().map(BranchView::of).toList(); }

    @PostMapping("/branches") @ResponseStatus(HttpStatus.CREATED)
    BranchView createBranch(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateBranch request) {
        return BranchView.of(service.createBranch(jwt, request.code(), request.name(), request.timezone()));
    }

    @PutMapping("/branches/{branchId}")
    BranchView updateBranch(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID branchId,@Valid @RequestBody BranchUpdate request) {
        return BranchView.of(service.updateBranch(jwt,branchId,request.name(),request.timezone(),request.status()));
    }

    @GetMapping("/branches/{branchId}/warehouses")
    List<WarehouseView> warehouses(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId) {
        return service.warehouses(jwt, branchId).stream().map(WarehouseView::of).toList();
    }

    @PostMapping("/branches/{branchId}/warehouses") @ResponseStatus(HttpStatus.CREATED)
    WarehouseView createWarehouse(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID branchId, @Valid @RequestBody CreateWarehouse request) {
        return WarehouseView.of(service.createWarehouse(jwt, branchId, request.code(), request.name()));
    }

    @PutMapping("/branches/{branchId}/warehouses/{id}")
    WarehouseView updateWarehouse(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID branchId,@PathVariable UUID id,
                                  @Valid @RequestBody WarehouseUpdate request) {
        return WarehouseView.of(service.updateWarehouse(jwt,branchId,id,request.name(),request.status()));
    }

    @GetMapping("/branches/{branchId}/departments")
    List<DepartmentView> departments(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID branchId) {
        return service.departments(jwt,branchId).stream().map(DepartmentView::of).toList();
    }

    @PostMapping("/branches/{branchId}/departments") @ResponseStatus(HttpStatus.CREATED)
    DepartmentView createDepartment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID branchId,
                                    @Valid @RequestBody DepartmentRequest request) {
        return DepartmentView.of(service.createDepartment(jwt,branchId,request.code(),request.name()));
    }

    @PutMapping("/branches/{branchId}/departments/{id}")
    DepartmentView updateDepartment(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID branchId,@PathVariable UUID id,
                                    @Valid @RequestBody DepartmentUpdate request) {
        return DepartmentView.of(service.updateDepartment(jwt,branchId,id,request.name(),request.status()));
    }
}
