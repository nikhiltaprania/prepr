package com.springrag.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * JWT signing configuration, bound from {@code security.jwt.*}. The secret MUST be overridden in
 * production via {@code SECURITY_JWT_SECRET} (an env var / secret manager) — the baked-in default is
 * a dev placeholder only. HS256 requires at least a 256-bit (32-byte) secret.
 */
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {

    /** HMAC signing secret. Override in production. Must be >= 32 bytes for HS256. */
    private String secret = "dev-only-insecure-secret-change-me-please-32-bytes-minimum!!";

    /** Access-token lifetime. */
    private Duration accessTokenTtl = Duration.ofHours(12);

    /** Token issuer claim. */
    private String issuer = "spring-rag";

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getAccessTokenTtl() {
        return accessTokenTtl;
    }

    public void setAccessTokenTtl(Duration accessTokenTtl) {
        this.accessTokenTtl = accessTokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }
}
