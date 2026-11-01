package com.erp.config;

import java.time.Instant;
import java.util.Base64;
import java.util.List;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import static org.junit.jupiter.api.Assertions.*;

class JwtValidationTest {
    private final ApiSecurityConfig config=new ApiSecurityConfig();
    private final SecretKey key=ApiSecurityConfig.signingKey(Base64.getEncoder().encodeToString(new byte[32]));
    private final String issuer="universal-erp-test",audience="erp-test";
    private final JdbcTemplate activeSession=new JdbcTemplate() {
        @Override @SuppressWarnings("unchecked")
        public <T> T queryForObject(String sql,Class<T> type,Object... args) { return (T)Boolean.TRUE; }
    };
    private final JwtDecoder decoder=config.jwtDecoder(key,activeSession,issuer,audience);

    @Test void verifiesLocallySignedTokenAndSession() {
        assertEquals("00000000-0000-0000-0000-000000000001",
                decoder.decode(token(key,issuer,audience,Instant.now().plusSeconds(300))).getSubject());
    }
    @Test void rejectsWrongAudience() {
        assertThrows(JwtException.class,()->decoder.decode(token(key,issuer,"other",Instant.now().plusSeconds(300))));
    }
    @Test void rejectsWrongIssuer() {
        assertThrows(JwtException.class,()->decoder.decode(token(key,"other",audience,Instant.now().plusSeconds(300))));
    }
    @Test void rejectsExpiredToken() {
        assertThrows(JwtException.class,()->decoder.decode(token(key,issuer,audience,Instant.now().minusSeconds(300))));
    }
    @Test void rejectsInvalidSignature() {
        byte[] other=new byte[32];other[0]=1;
        SecretKey another=ApiSecurityConfig.signingKey(Base64.getEncoder().encodeToString(other));
        assertThrows(JwtException.class,()->decoder.decode(token(another,issuer,audience,Instant.now().plusSeconds(300))));
    }
    @Test void rejectsRevokedSession() {
        JdbcTemplate revoked=new JdbcTemplate() {
            @Override @SuppressWarnings("unchecked")
            public <T> T queryForObject(String sql,Class<T> type,Object... args) { return (T)Boolean.FALSE; }
        };
        JwtDecoder revokedDecoder=config.jwtDecoder(key,revoked,issuer,audience);
        assertThrows(JwtException.class,()->revokedDecoder.decode(token(key,issuer,audience,Instant.now().plusSeconds(300))));
    }
    @Test void rejectsShortSecret() {
        assertThrows(IllegalStateException.class,()->ApiSecurityConfig.signingKey(Base64.getEncoder().encodeToString(new byte[16])));
    }
    private String token(SecretKey signingKey,String tokenIssuer,String tokenAudience,Instant expiry) {
        JwtEncoder encoder=config.jwtEncoder(signingKey);
        JwtClaimsSet claims=JwtClaimsSet.builder().issuer(tokenIssuer).audience(List.of(tokenAudience))
                .subject("00000000-0000-0000-0000-000000000001")
                .issuedAt(Instant.now().minusSeconds(600)).expiresAt(expiry).claim("sv",0).build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(),claims)).getTokenValue();
    }
}
