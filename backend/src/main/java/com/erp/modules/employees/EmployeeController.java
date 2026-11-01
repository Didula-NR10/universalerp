package com.erp.modules.employees;

import java.time.LocalDate;
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

@RestController @Profile("!foundation") @RequestMapping("/api/v1/employees")
class EmployeeController {
    private final EmployeeService service;
    EmployeeController(EmployeeService service) { this.service=service; }
    record CreateRequest(@NotBlank @Size(max=40) String employeeNumber,@NotBlank @Size(max=200) String fullName,
                         @Email @Size(max=320) String email,@NotBlank @Size(max=120) String jobPosition,UUID userId) {}
    record UpdateRequest(@NotBlank @Size(max=200) String fullName,@Email @Size(max=320) String email,
                         @NotBlank @Size(max=120) String jobPosition,@Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    record LeaveRequest(@NotNull LocalDate startDate,@NotNull LocalDate endDate,@NotBlank @Size(max=500) String reason) {}

    @GetMapping
    List<EmployeeService.EmployeeView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                            @RequestParam(defaultValue="") String search,
                                            @RequestParam(defaultValue="25") int limit,@RequestParam(defaultValue="0") int offset) {
        return service.list(jwt,branchId,search,Math.min(Math.max(limit,1),100),Math.max(offset,0));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    EmployeeService.EmployeeView create(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@Valid @RequestBody CreateRequest request) {
        return service.create(jwt,branchId,request.employeeNumber(),request.fullName(),request.email(),request.jobPosition(),request.userId());
    }
    @PutMapping("/{id}")
    EmployeeService.EmployeeView update(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,
                                         @Valid @RequestBody UpdateRequest request) {
        return service.update(jwt,branchId,id,request.fullName(),request.email(),request.jobPosition(),request.status());
    }
    @PostMapping("/{id}/attendance/check-in") @ResponseStatus(HttpStatus.CREATED)
    EmployeeService.AttendanceView checkIn(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,@RequestParam LocalDate date) {
        return service.checkIn(jwt,branchId,id,date);
    }
    @PostMapping("/{id}/attendance/check-out")
    EmployeeService.AttendanceView checkOut(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,@RequestParam LocalDate date) {
        return service.checkOut(jwt,branchId,id,date);
    }
    @PostMapping("/{id}/leave") @ResponseStatus(HttpStatus.CREATED)
    EmployeeService.LeaveView requestLeave(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id,
                                           @Valid @RequestBody LeaveRequest request) {
        return service.requestLeave(jwt,branchId,id,request.startDate(),request.endDate(),request.reason());
    }
    @GetMapping("/leave")
    List<EmployeeService.LeaveView> leaves(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                                           @RequestParam(defaultValue="25") int limit,@RequestParam(defaultValue="0") int offset) {
        return service.leaves(jwt,branchId,Math.min(Math.max(limit,1),100),Math.max(offset,0));
    }
    @PostMapping("/leave/{id}/approve")
    EmployeeService.LeaveView approve(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.decide(jwt,branchId,id,true);
    }
    @PostMapping("/leave/{id}/reject")
    EmployeeService.LeaveView reject(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,@PathVariable UUID id) {
        return service.decide(jwt,branchId,id,false);
    }
}
