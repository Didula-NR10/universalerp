package com.erp.modules.identity;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import com.erp.security.AccessService;

@RestController @Profile("!foundation") @RequestMapping("/api/v1")
class IdentityController {
    private final IdentityService service;
    private final AccessService access;
    IdentityController(IdentityService service,AccessService access) { this.service=service;this.access=access; }
    record Me(IdentityService.UserView user,List<String> permissions) {}
    record UserRequest(@NotBlank @Email String email,@NotBlank @Size(max=160) String displayName,
                       @NotBlank @Size(min=12,max=128) String temporaryPassword) {}
    record StatusRequest(@NotBlank @Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    record ResetPasswordRequest(@NotBlank @Size(min=12,max=128) String temporaryPassword) {}
    record RoleRequest(@NotBlank @Pattern(regexp="[A-Z][A-Z0-9_]{2,63}") String code,@NotBlank @Size(max=300) String description) {}
    record AssignmentRequest(@NotNull UUID userId,@NotBlank String roleCode,UUID branchId) {}
    record PermissionRequest(@NotBlank String permissionCode) {}

    @GetMapping("/me")
    Me me(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId) {
        return new Me(service.me(jwt),access.permissions(jwt,branchId));
    }
    @GetMapping("/admin/users")
    List<IdentityService.UserView> users(@AuthenticationPrincipal Jwt jwt) { return service.users(jwt); }
    @PostMapping("/admin/users") @ResponseStatus(HttpStatus.CREATED)
    IdentityService.UserView createUser(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody UserRequest request) {
        return service.createUser(jwt,request.email(),request.displayName(),request.temporaryPassword());
    }
    @PutMapping("/admin/users/{id}/status")
    IdentityService.UserView status(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody StatusRequest request) {
        return service.setStatus(jwt,id,request.status());
    }
    @PutMapping("/admin/users/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    void resetPassword(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(jwt,id,request.temporaryPassword());
    }
    @GetMapping("/admin/roles")
    List<IdentityService.RoleView> roles(@AuthenticationPrincipal Jwt jwt) { return service.roles(jwt); }
    @GetMapping("/admin/permissions")
    List<String> permissions(@AuthenticationPrincipal Jwt jwt) { return service.permissionCodes(jwt); }
    @PostMapping("/admin/roles") @ResponseStatus(HttpStatus.CREATED)
    IdentityService.RoleView role(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody RoleRequest request) {
        return service.createRole(jwt,request.code(),request.description());
    }
    @PostMapping("/admin/roles/{id}/permissions") @ResponseStatus(HttpStatus.NO_CONTENT)
    void grant(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id,@Valid @RequestBody PermissionRequest request) {
        service.grantPermission(jwt,id,request.permissionCode());
    }
    @PostMapping("/admin/assignments") @ResponseStatus(HttpStatus.CREATED)
    IdentityService.Assignment assign(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody AssignmentRequest request) {
        return service.assign(jwt,request.userId(),request.roleCode(),request.branchId());
    }
}
