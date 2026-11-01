package com.erp.modules.reporting;

import java.time.LocalDate;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/reports")
class ReportingController {
    private final ReportingService service;
    ReportingController(ReportingService service) { this.service=service; }
    @GetMapping("/overview")
    ReportingService.Overview overview(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                       @RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam String currency) {
        return service.overview(jwt,branchId,from,to,currency);
    }
    @GetMapping(value="/sales.csv",produces="text/csv")
    ResponseEntity<String> csv(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                               @RequestParam LocalDate from,@RequestParam LocalDate to,@RequestParam String currency) {
        var report=service.overview(jwt,branchId,from,to,currency);
        StringBuilder csv=new StringBuilder("date,currency,sales_total,sale_count\r\n");
        for (var day:report.dailySales()) csv.append(day.day()).append(',').append(report.currency())
                .append(',').append(day.total()).append(',').append(day.count()).append("\r\n");
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"sales-report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv")).body(csv.toString());
    }
}
