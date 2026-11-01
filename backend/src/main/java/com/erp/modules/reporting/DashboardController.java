package com.erp.modules.reporting;

import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/dashboard")
class DashboardController {
    private final DashboardService service;
    DashboardController(DashboardService service) { this.service=service; }
    @GetMapping
    DashboardService.Dashboard dashboard(@AuthenticationPrincipal Jwt jwt,@RequestParam(required=false) UUID branchId) {
        return service.load(jwt,branchId);
    }
}
