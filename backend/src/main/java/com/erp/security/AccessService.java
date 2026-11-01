package com.erp.security;

import java.util.Map;
import java.util.UUID;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
@Profile("!foundation")
public class AccessService {
    private final NamedParameterJdbcTemplate jdbc;

    public AccessService(NamedParameterJdbcTemplate jdbc) { this.jdbc = jdbc; }

    public UUID userId(Jwt jwt) {
        try { return UUID.fromString(jwt.getSubject()); }
        catch (RuntimeException ex) { throw new AccessDeniedException("Invalid user identity"); }
    }

    public void require(Jwt jwt, String permission, UUID branchId) {
        UUID userId = userId(jwt);
        String sql = """
            SELECT EXISTS (
              SELECT 1 FROM user_profile u
              JOIN local_credential c ON c.user_id = u.auth_user_id
              JOIN user_role ur ON ur.user_id = u.auth_user_id
              JOIN erp_role r ON r.id = ur.role_id
              JOIN role_permission rp ON rp.role_id = r.id
              WHERE u.auth_user_id = :userId AND u.status = 'ACTIVE' AND c.must_change_password = false
                AND rp.permission_code = :permission
                AND ((ur.branch_id IS NULL AND r.code IN ('SUPER_ADMIN', 'COMPANY_ADMIN', 'AUDITOR'))
            """ + (branchId == null ? "" : """
                  OR (ur.branch_id = :branchId AND EXISTS
                    (SELECT 1 FROM user_branch ub JOIN branch b ON b.id=ub.branch_id
                     WHERE ub.user_id = u.auth_user_id AND ub.branch_id = :branchId AND b.status='ACTIVE'))
            """) + """
                )
            )
            """;
        Map<String, ?> params = branchId == null
                ? Map.of("userId", userId, "permission", permission)
                : Map.of("userId", userId, "permission", permission, "branchId", branchId);
        Boolean allowed = jdbc.queryForObject(sql, params, Boolean.class);
        if (!Boolean.TRUE.equals(allowed)) throw new AccessDeniedException("Permission denied");
    }

    public List<UUID> visibleBranches(Jwt jwt) {
        UUID userId = userId(jwt);
        Boolean companyWide = jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM user_profile u JOIN local_credential c ON c.user_id=u.auth_user_id
                JOIN user_role ur ON ur.user_id=u.auth_user_id
                JOIN erp_role r ON r.id=ur.role_id JOIN role_permission rp ON rp.role_id=r.id
                WHERE u.auth_user_id=:userId AND u.status='ACTIVE' AND c.must_change_password=false AND ur.branch_id IS NULL
                AND r.code IN ('SUPER_ADMIN','COMPANY_ADMIN','AUDITOR') AND rp.permission_code='BRANCH_READ')
                """, Map.of("userId", userId), Boolean.class);
        if (Boolean.TRUE.equals(companyWide))
            return jdbc.query("SELECT id FROM branch ORDER BY name", (rs, row) -> rs.getObject(1, UUID.class));
        return jdbc.query("""
                SELECT DISTINCT b.id FROM branch b JOIN user_branch ub ON ub.branch_id=b.id
                JOIN user_role ur ON ur.user_id=ub.user_id AND ur.branch_id=b.id
                JOIN role_permission rp ON rp.role_id=ur.role_id
                JOIN user_profile u ON u.auth_user_id=ub.user_id
                JOIN local_credential c ON c.user_id=u.auth_user_id
                WHERE ub.user_id=:userId AND u.status='ACTIVE' AND c.must_change_password=false AND b.status='ACTIVE'
                AND rp.permission_code='BRANCH_READ' ORDER BY b.id
                """, Map.of("userId", userId), (rs, row) -> rs.getObject(1, UUID.class));
    }

    public boolean isSuperAdmin(Jwt jwt) {
        Boolean found=jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM user_profile u JOIN user_role ur ON ur.user_id=u.auth_user_id
                JOIN erp_role r ON r.id=ur.role_id WHERE u.auth_user_id=:userId AND u.status='ACTIVE'
                AND ur.branch_id IS NULL AND r.code='SUPER_ADMIN')
                """,Map.of("userId",userId(jwt)),Boolean.class);
        return Boolean.TRUE.equals(found);
    }

    public List<String> permissions(Jwt jwt,UUID branchId) {
        UUID userId=userId(jwt);
        String sql="SELECT DISTINCT rp.permission_code FROM user_profile u "
                +"JOIN local_credential c ON c.user_id=u.auth_user_id "
                +"JOIN user_role ur ON ur.user_id=u.auth_user_id "
                +"JOIN erp_role r ON r.id=ur.role_id JOIN role_permission rp ON rp.role_id=r.id "
                +"WHERE u.auth_user_id=:userId AND u.status='ACTIVE' AND c.must_change_password=false "
                +"AND ((ur.branch_id IS NULL AND r.code IN ('SUPER_ADMIN','COMPANY_ADMIN','AUDITOR'))"
                +(branchId==null?"":" OR (ur.branch_id=:branchId AND EXISTS(SELECT 1 FROM user_branch ub JOIN branch b ON b.id=ub.branch_id WHERE ub.user_id=u.auth_user_id AND ub.branch_id=:branchId AND b.status='ACTIVE'))")
                +") ORDER BY rp.permission_code";
        return jdbc.query(sql,branchId==null?Map.of("userId",userId):Map.of("userId",userId,"branchId",branchId),
                (rs,row)->rs.getString(1));
    }
}
