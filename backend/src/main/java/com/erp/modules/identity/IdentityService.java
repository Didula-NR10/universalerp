package com.erp.modules.identity;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import com.erp.security.AccessService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class IdentityService {
    record UserView(UUID id,String email,String displayName,String status) {}
    record RoleView(UUID id,String code,String description,boolean systemRole) {}
    record Assignment(UUID userId,String roleCode,UUID branchId) {}
    private final JdbcTemplate jdbc;
    private final AccessService access;
    private final PasswordEncoder passwords;
    IdentityService(JdbcTemplate jdbc,AccessService access,PasswordEncoder passwords) {
        this.jdbc=jdbc;this.access=access;this.passwords=passwords;
    }
    private UserView mapUser(ResultSet rs,int row) throws SQLException {
        return new UserView(rs.getObject("auth_user_id",UUID.class),rs.getString("email"),rs.getString("display_name"),rs.getString("status"));
    }
    @Transactional(readOnly=true)
    UserView me(Jwt jwt) {
        List<UserView> rows=jdbc.query("SELECT * FROM user_profile WHERE auth_user_id=?",this::mapUser,access.userId(jwt));
        if (rows.isEmpty()) throw new EntityNotFoundException("ERP user profile not found");
        if (!"ACTIVE".equals(rows.get(0).status())) throw new AccessDeniedException("ERP user is inactive");
        return rows.get(0);
    }
    @Transactional(readOnly=true)
    List<UserView> users(Jwt jwt) {
        access.require(jwt,"USER_MANAGE",null);
        return jdbc.query("SELECT * FROM user_profile ORDER BY display_name LIMIT 200",this::mapUser);
    }
    @Transactional
    UserView createUser(Jwt jwt,String email,String displayName,String temporaryPassword) {
        access.require(jwt,"USER_MANAGE",null);
        if (temporaryPassword==null || temporaryPassword.length()<12 || temporaryPassword.length()>128)
            throw new IllegalArgumentException("Temporary password must be 12 to 128 characters");
        UUID authUserId=UUID.randomUUID();
        jdbc.update("INSERT INTO user_profile(auth_user_id,email,display_name) VALUES (?,?,?)",
                authUserId,email.trim().toLowerCase(),displayName.trim());
        jdbc.update("INSERT INTO local_credential(user_id,password_hash) VALUES (?,?)",
                authUserId,passwords.encode(temporaryPassword));
        audit(jwt,"USER_CREATED","USER",authUserId.toString());
        return jdbc.queryForObject("SELECT * FROM user_profile WHERE auth_user_id=?",this::mapUser,authUserId);
    }
    @Transactional
    UserView setStatus(Jwt jwt,UUID userId,String status) {
        access.require(jwt,"USER_MANAGE",null);
        if (userId.equals(access.userId(jwt))) throw new AccessDeniedException("Cannot deactivate own profile");
        int updated=jdbc.update("UPDATE user_profile SET status=?,updated_at=now(),version=version+1 WHERE auth_user_id=?",status,userId);
        if (updated==0) throw new EntityNotFoundException("User not found");
        jdbc.update("UPDATE local_credential SET session_version=session_version+1,updated_at=now() WHERE user_id=?",userId);
        audit(jwt,"USER_STATUS_CHANGED","USER",userId.toString());
        return jdbc.queryForObject("SELECT * FROM user_profile WHERE auth_user_id=?",this::mapUser,userId);
    }
    @Transactional
    void resetPassword(Jwt jwt,UUID userId,String temporaryPassword) {
        access.require(jwt,"USER_MANAGE",null);
        if (userId.equals(access.userId(jwt))) throw new AccessDeniedException("Use change password for your own account");
        if (temporaryPassword==null || temporaryPassword.length()<12 || temporaryPassword.length()>128)
            throw new IllegalArgumentException("Temporary password must be 12 to 128 characters");
        int updated=jdbc.update("""
                UPDATE local_credential SET password_hash=?,must_change_password=true,
                password_changed_at=NULL,session_version=session_version+1,
                failed_attempts=0,locked_until=NULL,updated_at=now() WHERE user_id=?
                """,passwords.encode(temporaryPassword),userId);
        if (updated==0) throw new EntityNotFoundException("Local user not found");
        audit(jwt,"USER_PASSWORD_RESET","USER",userId.toString());
    }
    @Transactional(readOnly=true)
    List<RoleView> roles(Jwt jwt) {
        access.require(jwt,"ROLE_MANAGE",null);
        return jdbc.query("SELECT * FROM erp_role ORDER BY code",(rs,row) -> new RoleView(rs.getObject("id",UUID.class),
                rs.getString("code"),rs.getString("description"),rs.getBoolean("system_role")));
    }
    @Transactional(readOnly=true)
    List<String> permissionCodes(Jwt jwt) {
        access.require(jwt,"ROLE_MANAGE",null);
        return jdbc.query("SELECT code FROM erp_permission ORDER BY code",(rs,row)->rs.getString(1));
    }
    @Transactional
    RoleView createRole(Jwt jwt,String code,String description) {
        access.require(jwt,"ROLE_MANAGE",null);
        UUID id=UUID.randomUUID();String normalized=code.trim().toUpperCase();
        jdbc.update("INSERT INTO erp_role(id,code,description,system_role) VALUES (?,?,?,false)",id,normalized,description.trim());
        audit(jwt,"ROLE_CREATED","ROLE",id.toString());
        return new RoleView(id,normalized,description,false);
    }
    @Transactional
    void grantPermission(Jwt jwt,UUID roleId,String permission) {
        access.require(jwt,"ROLE_MANAGE",null);
        Integer ownAssignments=jdbc.queryForObject("SELECT count(*) FROM user_role WHERE user_id=? AND role_id=?",Integer.class,access.userId(jwt),roleId);
        if (ownAssignments!=null && ownAssignments>0) throw new AccessDeniedException("Cannot change own role permissions");
        Boolean custom=jdbc.queryForObject("SELECT NOT system_role FROM erp_role WHERE id=?",Boolean.class,roleId);
        if (!Boolean.TRUE.equals(custom)) throw new IllegalArgumentException("Built-in roles cannot be changed");
        jdbc.update("INSERT INTO role_permission(role_id,permission_code) VALUES (?,?) ON CONFLICT DO NOTHING",roleId,permission);
        audit(jwt,"ROLE_PERMISSION_GRANTED","ROLE",roleId.toString());
    }
    @Transactional
    Assignment assign(Jwt jwt,UUID userId,String roleCode,UUID branchId) {
        access.require(jwt,"ROLE_MANAGE",null);
        if (userId.equals(access.userId(jwt))) throw new AccessDeniedException("Cannot assign a role to yourself");
        List<RoleView> roles=jdbc.query("SELECT * FROM erp_role WHERE code=?",(rs,row) -> new RoleView(rs.getObject("id",UUID.class),
                rs.getString("code"),rs.getString("description"),rs.getBoolean("system_role")),roleCode);
        if (roles.isEmpty()) throw new EntityNotFoundException("Role not found");
        RoleView role=roles.get(0);
        boolean global=roleCode.equals("SUPER_ADMIN")||roleCode.equals("COMPANY_ADMIN")||roleCode.equals("AUDITOR");
        if (global != (branchId==null)) throw new IllegalArgumentException("Invalid role scope");
        if (roleCode.equals("SUPER_ADMIN")&&!access.isSuperAdmin(jwt)) throw new AccessDeniedException("Super admin required");
        Integer userCount=jdbc.queryForObject("SELECT count(*) FROM user_profile WHERE auth_user_id=? AND status='ACTIVE'",Integer.class,userId);
        if (userCount==null||userCount==0) throw new EntityNotFoundException("Active user not found");
        if (branchId!=null) {
            Integer branchCount=jdbc.queryForObject("SELECT count(*) FROM branch WHERE id=? AND status='ACTIVE'",Integer.class,branchId);
            if (branchCount==null||branchCount==0) throw new EntityNotFoundException("Active branch not found");
            jdbc.update("INSERT INTO user_branch(user_id,branch_id) VALUES (?,?) ON CONFLICT DO NOTHING",userId,branchId);
        }
        jdbc.update("INSERT INTO user_role(user_id,role_id,branch_id) VALUES (?,?,?) ON CONFLICT DO NOTHING",userId,role.id(),branchId);
        audit(jwt,"USER_ROLE_ASSIGNED","USER",userId.toString());
        return new Assignment(userId,roleCode,branchId);
    }
    private void audit(Jwt jwt,String action,String resourceType,String resourceId) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,action,resource_type,resource_id,outcome) VALUES (?,?,?,?,'SUCCESS')",
                access.userId(jwt),action,resourceType,resourceId);
    }
}
