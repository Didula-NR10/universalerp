package com.erp.modules.identity;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service @Profile("!foundation")
class LocalAuthService {
    record User(UUID id,String email,String displayName,String status) {}
    record Session(String accessToken,Instant expiresAt,User user,boolean mustChangePassword) {}
    private record Credential(User user,String hash,int failures,Instant lockedUntil,long sessionVersion,boolean mustChangePassword) {}
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwords;
    private final JwtEncoder jwtEncoder;
    private final String issuer;
    private final String audience;
    private final String dummyHash;

    LocalAuthService(JdbcTemplate jdbc,PasswordEncoder passwords,JwtEncoder jwtEncoder,
                     @Value("${erp.auth.issuer}") String issuer,@Value("${erp.auth.audience}") String audience) {
        this.jdbc=jdbc;this.passwords=passwords;this.jwtEncoder=jwtEncoder;this.issuer=issuer;this.audience=audience;
        this.dummyHash=passwords.encode(UUID.randomUUID().toString());
    }

    @Transactional(noRollbackFor=BadCredentialsException.class)
    Session login(String email,String password) {
        String normalized=email==null?"":email.trim().toLowerCase();
        List<Credential> rows=jdbc.query("""
                SELECT p.auth_user_id,p.email,p.display_name,p.status,c.password_hash,c.failed_attempts,
                       c.locked_until,c.session_version,c.must_change_password
                FROM user_profile p JOIN local_credential c ON c.user_id=p.auth_user_id
                WHERE lower(p.email)=? FOR UPDATE OF c
                """,(rs,row)->new Credential(
                new User(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)),
                rs.getString(5),rs.getInt(6),rs.getTimestamp(7)==null?null:rs.getTimestamp(7).toInstant(),
                rs.getLong(8),rs.getBoolean(9)),normalized);
        if (rows.isEmpty()) {
            passwords.matches(password==null?"":password,dummyHash);
            throw invalid();
        }
        Credential credential=rows.get(0);
        if (!"ACTIVE".equals(credential.user().status()) || credential.lockedUntil()!=null
                && credential.lockedUntil().isAfter(Instant.now())) {
            passwords.matches(password==null?"":password,dummyHash);
            throw invalid();
        }
        if (password==null || !passwords.matches(password,credential.hash())) {
            int failures=Math.min(5,credential.failures()+1);
            Timestamp locked=failures>=5?Timestamp.from(Instant.now().plus(Duration.ofMinutes(15))):null;
            jdbc.update("UPDATE local_credential SET failed_attempts=?,locked_until=?,updated_at=now() WHERE user_id=?",
                    failures,locked,credential.user().id());
            throw invalid();
        }
        jdbc.update("UPDATE local_credential SET failed_attempts=0,locked_until=NULL,updated_at=now() WHERE user_id=?",
                credential.user().id());
        audit(credential.user().id(),"LOGIN_SUCCESS");
        return issue(credential.user(),credential.sessionVersion(),credential.mustChangePassword());
    }

    @Transactional(readOnly=true)
    Session session(Jwt jwt) {
        UUID userId=UUID.fromString(jwt.getSubject());
        Credential credential=credential(userId,false);
        return new Session(jwt.getTokenValue(),jwt.getExpiresAt(),credential.user(),credential.mustChangePassword());
    }

    @Transactional
    void logout(Jwt jwt) {
        UUID userId=UUID.fromString(jwt.getSubject());
        jdbc.update("UPDATE local_credential SET session_version=session_version+1,updated_at=now() WHERE user_id=?",userId);
        audit(userId,"LOGOUT");
    }

    @Transactional
    void changePassword(Jwt jwt,String currentPassword,String newPassword) {
        if (newPassword==null || newPassword.length()<12 || newPassword.length()>128)
            throw new IllegalArgumentException("New password must be 12 to 128 characters");
        UUID userId=UUID.fromString(jwt.getSubject());
        Credential credential=credential(userId,true);
        if (currentPassword==null || !passwords.matches(currentPassword,credential.hash())) throw invalid();
        if (passwords.matches(newPassword,credential.hash())) throw new IllegalArgumentException("Choose a different password");
        jdbc.update("""
                UPDATE local_credential SET password_hash=?,must_change_password=false,
                password_changed_at=now(),session_version=session_version+1,
                failed_attempts=0,locked_until=NULL,updated_at=now() WHERE user_id=?
                """,passwords.encode(newPassword),userId);
        audit(userId,"PASSWORD_CHANGED");
    }

    private Credential credential(UUID userId,boolean lock) {
        List<Credential> rows=jdbc.query("""
                SELECT p.auth_user_id,p.email,p.display_name,p.status,c.password_hash,c.failed_attempts,
                       c.locked_until,c.session_version,c.must_change_password
                FROM user_profile p JOIN local_credential c ON c.user_id=p.auth_user_id
                WHERE p.auth_user_id=?
                """+(lock?" FOR UPDATE OF c":""),(rs,row)->new Credential(
                new User(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4)),
                rs.getString(5),rs.getInt(6),rs.getTimestamp(7)==null?null:rs.getTimestamp(7).toInstant(),
                rs.getLong(8),rs.getBoolean(9)),userId);
        if (rows.isEmpty()) throw invalid();
        return rows.get(0);
    }

    private Session issue(User user,long version,boolean mustChangePassword) {
        Instant now=Instant.now(),expires=now.plus(Duration.ofHours(8));
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                .subject(user.id().toString()).issuedAt(now).expiresAt(expires).claim("sv",version).build();
        String token=jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
        return new Session(token,expires,user,mustChangePassword);
    }

    private BadCredentialsException invalid() { return new BadCredentialsException("Invalid email or password"); }

    private void audit(UUID userId,String action) {
        jdbc.update("INSERT INTO audit_log(actor_user_id,action,resource_type,resource_id,outcome) VALUES (?,?,'AUTH',?,'SUCCESS')",
                userId,action,userId.toString());
    }
}
