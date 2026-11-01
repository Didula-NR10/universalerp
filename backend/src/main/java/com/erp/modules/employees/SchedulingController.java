package com.erp.modules.employees;

import java.time.Instant;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.context.annotation.Profile;
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

@RestController @Profile("!foundation") @RequestMapping("/api/v1/schedules")
class SchedulingController {
    private final SchedulingService service;
    SchedulingController(SchedulingService service) { this.service=service; }
    record TemplateRequest(@NotBlank @Size(max=32) String code,@NotBlank @Size(max=160) String name,
                           @NotNull LocalTime startTime,@NotNull LocalTime endTime,@Min(0) @Max(480) int breakMinutes) {}
    record AvailabilityRequest(@Min(1) @Max(7) int weekday,@NotNull LocalTime startTime,@NotNull LocalTime endTime) {}
    record ShiftRequest(@NotNull UUID employeeId,UUID templateId,@NotNull Instant startAt,@NotNull Instant endAt) {}

    @GetMapping("/me")
    UUID ownEmployee(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId) {
        return service.ownEmployeeId(jwt,branchId);
    }

    @GetMapping("/templates")
    List<SchedulingService.Template> templates(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId) {
        return service.templates(jwt,branchId);
    }
    @PostMapping("/templates") @ResponseStatus(HttpStatus.CREATED)
    SchedulingService.Template createTemplate(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                               @Valid @RequestBody TemplateRequest request) {
        return service.createTemplate(jwt,branchId,request.code(),request.name(),request.startTime(),request.endTime(),request.breakMinutes());
    }
    @GetMapping("/availability/{employeeId}")
    List<SchedulingService.Availability> availability(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                                       @PathVariable UUID employeeId) {
        return service.availability(jwt,branchId,employeeId);
    }
    @PostMapping("/availability/{employeeId}") @ResponseStatus(HttpStatus.CREATED)
    SchedulingService.Availability addAvailability(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                                   @PathVariable UUID employeeId,@Valid @RequestBody AvailabilityRequest request) {
        return service.addAvailability(jwt,branchId,employeeId,request.weekday(),request.startTime(),request.endTime());
    }
    @GetMapping("/shifts")
    List<SchedulingService.Shift> shifts(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                        @RequestParam Instant from,@RequestParam Instant to) {
        return service.shifts(jwt,branchId,from,to);
    }
    @PostMapping("/shifts") @ResponseStatus(HttpStatus.CREATED)
    SchedulingService.Shift createShift(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@Valid @RequestBody ShiftRequest request) {
        return service.createShift(jwt,branchId,request.employeeId(),request.templateId(),request.startAt(),request.endAt());
    }
    @PostMapping("/shifts/{id}/approve")
    SchedulingService.Shift approve(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.approve(jwt,branchId,id);
    }
    @PostMapping("/shifts/{id}/cancel")
    SchedulingService.Shift cancel(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.cancel(jwt,branchId,id);
    }
}
