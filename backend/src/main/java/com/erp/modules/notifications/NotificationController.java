package com.erp.modules.notifications;

import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
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
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/notifications")
class NotificationController {
    private final NotificationService service;
    NotificationController(NotificationService service) { this.service=service; }
    record Preference(@NotBlank String type,boolean enabled) {}
    @GetMapping
    List<NotificationService.NotificationView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam(defaultValue="50") int limit) {
        return service.list(jwt,Math.min(Math.max(limit,1),100));
    }
    @PostMapping("/{id}/read") @ResponseStatus(HttpStatus.NO_CONTENT)
    void read(@AuthenticationPrincipal Jwt jwt,@PathVariable UUID id) { service.markRead(jwt,id); }
    @PutMapping("/preference") @ResponseStatus(HttpStatus.NO_CONTENT)
    void preference(@AuthenticationPrincipal Jwt jwt,@Valid @RequestBody Preference request) {
        service.preference(jwt,request.type(),request.enabled());
    }
}
