package com.erp.config;

import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("foundation")
@RequestMapping("/api/v1/system")
class SystemStatusController {
    record StatusResponse(String status, String phase, Instant timestamp) {}

    @GetMapping("/status")
    StatusResponse status() {
        return new StatusResponse("available", "foundation", Instant.now());
    }
}
