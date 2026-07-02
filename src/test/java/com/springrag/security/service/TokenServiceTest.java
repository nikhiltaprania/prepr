package com.springrag.security.service;

import com.springrag.security.config.JwtProperties;
import com.springrag.user.domain.User;
import com.springrag.user.domain.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the JWT round-trip wired in SecurityConfig: a token issued by TokenService is accepted by
 * a matching HS256 decoder and carries the subject (user id) and role claim that authorization relies on.
 */
class TokenServiceTest {

    private static final String SECRET = "test-secret-key-that-is-at-least-32-bytes-long!!";

    @Test
    void issuedTokenDecodesWithSubjectAndRole() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);

        SecretKey key = new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        JwtEncoder encoder = new NimbusJwtEncoder(new com.nimbusds.jose.jwk.source.ImmutableSecret<>(key));
        JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();

        TokenService tokenService = new TokenService(encoder, properties);

        User user = new User();
        UUID userId = UUID.randomUUID();
        user.setId(userId);
        user.setUsername("Nikhil");
        user.setEmail("candidate@example.com");
        user.setRole(UserRole.USER);

        String token = tokenService.issueToken(user);
        Jwt decoded = decoder.decode(token);

        assertThat(decoded.getSubject()).isEqualTo(userId.toString());
        assertThat(decoded.getClaimAsString("role")).isEqualTo("ROLE_USER");
        assertThat(decoded.getClaimAsString("email")).isEqualTo("candidate@example.com");
        assertThat(decoded.getClaimAsString("name")).isEqualTo("Nikhil");
        assertThat(decoded.getExpiresAt()).isNotNull();
    }
}
