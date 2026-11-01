package com.erp.modules.contacts;

import java.time.Instant;
import java.util.UUID;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class ContactService {
    private final BusinessContactRepository contacts;
    private final AccessService access;
    private final JdbcTemplate jdbc;
    ContactService(BusinessContactRepository contacts,AccessService access,JdbcTemplate jdbc) {
        this.contacts=contacts; this.access=access; this.jdbc=jdbc;
    }
    @Transactional(readOnly=true)
    Page<BusinessContact> list(Jwt jwt,UUID branchId,String type,String search,Pageable pageable) {
        access.require(jwt,"CONTACT_READ",branchId);
        return contacts.findByBranchIdAndContactTypeAndNameContainingIgnoreCase(branchId,type,search,pageable);
    }
    @Transactional
    BusinessContact create(Jwt jwt,UUID branchId,String type,String name,String email,String phone) {
        access.require(jwt,"CONTACT_MANAGE",branchId);
        BusinessContact contact=contacts.save(new BusinessContact(branchId,type,name,email,phone));
        audit(jwt,branchId,"CONTACT_CREATED",contact.id);
        return contact;
    }
    @Transactional
    BusinessContact update(Jwt jwt,UUID branchId,UUID id,String type,String name,String email,String phone,String status) {
        access.require(jwt,"CONTACT_MANAGE",branchId);
        BusinessContact contact=contacts.findById(id).orElseThrow(() -> new EntityNotFoundException("Contact not found"));
        if (!contact.branchId.equals(branchId) || !contact.contactType.equals(type)) throw new EntityNotFoundException("Contact not found");
        contact.name=name.trim(); contact.email=email; contact.phone=phone;
        if (status != null) contact.status=status;
        contact.updatedAt=Instant.now(); audit(jwt,branchId,"CONTACT_UPDATED",contact.id);
        return contact;
    }
    private void audit(Jwt jwt,UUID branchId,String action,UUID id) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,branch_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,'CONTACT',?,'SUCCESS')",
                access.userId(jwt),branchId,action,id.toString());
    }
}
