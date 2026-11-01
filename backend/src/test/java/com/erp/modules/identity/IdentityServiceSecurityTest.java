package com.erp.modules.identity;

import java.util.UUID;
import com.erp.security.AccessService;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class IdentityServiceSecurityTest {
    private final JdbcTemplate jdbc=mock(JdbcTemplate.class);
    private final AccessService access=mock(AccessService.class);
    private final IdentityService service=new IdentityService(jdbc,access,mock(PasswordEncoder.class));
    private final Jwt jwt=mock(Jwt.class);
    private final UUID actor=UUID.randomUUID();

    @Test void administratorCannotAssignAnyRoleToSelf() {
        when(access.userId(jwt)).thenReturn(actor);
        assertThrows(AccessDeniedException.class,()->service.assign(jwt,actor,"COMPANY_ADMIN",null));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }

    @Test void administratorCannotEditPermissionsOfOwnedRole() {
        UUID roleId=UUID.randomUUID();
        when(access.userId(jwt)).thenReturn(actor);
        when(jdbc.queryForObject("SELECT count(*) FROM user_role WHERE user_id=? AND role_id=?",Integer.class,actor,roleId)).thenReturn(1);
        assertThrows(AccessDeniedException.class,()->service.grantPermission(jwt,roleId,"ROLE_MANAGE"));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }

    @Test void administratorCannotResetOwnPasswordThroughAdminEndpoint() {
        when(access.userId(jwt)).thenReturn(actor);
        assertThrows(AccessDeniedException.class,()->service.resetPassword(jwt,actor,"TemporaryPassword123!"));
        verify(jdbc,never()).update(anyString(),any(Object[].class));
    }
}
