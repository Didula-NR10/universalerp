package com.erp.modules.identity;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("postgres-test")
@EnabledIfEnvironmentVariable(named="TEST_DB_DISPOSABLE",matches="true")
class PostgresLocalAuthIntegrationTest {
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder passwords;
    @Autowired LocalAuthService auth;
    @Autowired JwtDecoder decoder;

    @Test @Transactional
    void passwordChangeRevokesSessionsAndRepeatedFailuresLockAccount() {
        UUID id=UUID.randomUUID();
        String email="local-"+id+"@example.invalid";
        jdbc.update("INSERT INTO user_profile(auth_user_id,email,display_name) VALUES (?,?,?)",id,email,"Local test user");
        jdbc.update("INSERT INTO local_credential(user_id,password_hash) VALUES (?,?)",id,passwords.encode("TemporaryPassword123!"));
        assertThrows(BadCredentialsException.class,()->auth.login(email,"wrong password"));
        var first=auth.login(email,"TemporaryPassword123!");
        assertTrue(first.mustChangePassword());
        assertEquals(id.toString(),decoder.decode(first.accessToken()).getSubject());

        auth.changePassword(decoder.decode(first.accessToken()),"TemporaryPassword123!","NewPrivatePassword456!");
        assertThrows(JwtException.class,()->decoder.decode(first.accessToken()));
        var second=auth.login(email,"NewPrivatePassword456!");
        assertFalse(second.mustChangePassword());
        auth.logout(decoder.decode(second.accessToken()));
        assertThrows(JwtException.class,()->decoder.decode(second.accessToken()));

        for (int attempt=0;attempt<5;attempt++)
            assertThrows(BadCredentialsException.class,()->auth.login(email,"wrong password"));
        assertThrows(BadCredentialsException.class,()->auth.login(email,"NewPrivatePassword456!"));
    }
}
