package com.erp.modules.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/auth")
class LocalAuthController {
    private final LocalAuthService service;
    LocalAuthController(LocalAuthService service) { this.service=service; }
    record LoginRequest(@NotBlank @Email String email,@NotBlank String password) {}
    record ChangePasswordRequest(@NotBlank String currentPassword,@NotBlank @Size(min=12,max=128) String newPassword) {}

    @PostMapping("/login")
    LocalAuthService.Session login(@Valid @RequestBody LoginRequest request) {
        return service.login(request.email(),request.password());
    }
    @GetMapping("/session")
    LocalAuthService.Session session(@AuthenticationPrincipal Jwt jwt) { return service.session(jwt); }
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    void logout(@AuthenticationPrincipal Jwt jwt) { service.logout(jwt); }
    @PostMapping("/change-password") @ResponseStatus(HttpStatus.NO_CONTENT)
    void changePassword(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody ChangePasswordRequest request) {
        service.changePassword(jwt,request.currentPassword(),request.newPassword());
    }
}
