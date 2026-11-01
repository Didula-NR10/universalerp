package com.erp.modules.contacts;

import java.util.UUID;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/contacts")
class ContactController {
    private final ContactService service;
    ContactController(ContactService service) { this.service=service; }
    record ContactRequest(@NotBlank @Size(max=200) String name,@Email @Size(max=320) String email,
                          @Size(max=40) String phone,@Pattern(regexp="ACTIVE|INACTIVE") String status) {}
    record ContactView(UUID id,UUID branchId,String type,String name,String email,String phone,String status) {
        static ContactView of(BusinessContact c) { return new ContactView(c.id,c.branchId,c.contactType,c.name,c.email,c.phone,c.status); }
    }
    private String type(String type) {
        if (!"SUPPLIER".equals(type) && !"CUSTOMER".equals(type)) throw new IllegalArgumentException("Invalid contact type");
        return type;
    }
    @GetMapping("/{type}")
    Page<ContactView> list(@AuthenticationPrincipal Jwt jwt,@PathVariable String type,@RequestParam UUID branchId,
                           @RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page,
                           @RequestParam(defaultValue="25") int size) {
        return service.list(jwt,branchId,type(type),search,PageRequest.of(page,Math.min(Math.max(size,1),100))).map(ContactView::of);
    }
    @PostMapping("/{type}") @ResponseStatus(HttpStatus.CREATED)
    ContactView create(@AuthenticationPrincipal Jwt jwt,@PathVariable String type,@RequestParam UUID branchId,
                       @Valid @RequestBody ContactRequest request) {
        return ContactView.of(service.create(jwt,branchId,type(type),request.name(),request.email(),request.phone()));
    }
    @PutMapping("/{type}/{id}")
    ContactView update(@AuthenticationPrincipal Jwt jwt,@PathVariable String type,@PathVariable UUID id,
                       @RequestParam UUID branchId,@Valid @RequestBody ContactRequest request) {
        return ContactView.of(service.update(jwt,branchId,id,type(type),request.name(),request.email(),request.phone(),request.status()));
    }
}
