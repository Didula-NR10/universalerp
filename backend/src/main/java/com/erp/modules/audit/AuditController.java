package com.erp.modules.audit;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import com.erp.security.AccessService;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController @Profile("!foundation") @RequestMapping("/api/v1/audit")
class AuditController {
    record AuditView(long id,UUID actorUserId,UUID branchId,String action,String resourceType,String resourceId,
                     String outcome,String summary,Instant occurredAt) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    AuditController(JdbcTemplate jdbc,AccessService access) { this.jdbc=jdbc;this.access=access; }
    @GetMapping
    List<AuditView> list(@AuthenticationPrincipal Jwt jwt,@RequestParam UUID branchId,
                         @RequestParam(defaultValue="50") int limit,@RequestParam(defaultValue="0") int offset) {
        access.require(jwt,"AUDIT_VIEW",branchId);
        return jdbc.query("SELECT * FROM audit_log WHERE branch_id=? ORDER BY occurred_at DESC LIMIT ? OFFSET ?",(rs,row)->
                new AuditView(rs.getLong("id"),rs.getObject("actor_user_id",UUID.class),rs.getObject("branch_id",UUID.class),
                        rs.getString("action"),rs.getString("resource_type"),rs.getString("resource_id"),
                        rs.getString("outcome"),rs.getString("summary"),rs.getTimestamp("occurred_at").toInstant()),
                branchId,Math.min(Math.max(limit,1),100),Math.max(offset,0));
    }
}
