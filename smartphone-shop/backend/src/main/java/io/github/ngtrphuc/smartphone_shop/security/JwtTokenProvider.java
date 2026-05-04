package io.github.ngtrphuc.smartphone_shop.security;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.crypto.SecretKey;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {

    private static final String ROLE_CLAIM = "role";
    private static final String INSECURE_SECRET_PREFIX = "change-this-secret-in-production";
    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);

    private final JwtProperties jwtProperties;
    private final SecretKey signingKey;

    public JwtTokenProvider(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("JWT secret must not be blank.");
        }
        boolean prodProfileActive = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile));
        if (prodProfileActive && secret.startsWith(INSECURE_SECRET_PREFIX)) {
            throw new IllegalStateException("JWT secret must be overridden in production.");
        }
        if (!prodProfileActive && secret.startsWith(INSECURE_SECRET_PREFIX)) {
            log.warn("Using default JWT secret for non-production profile. Set JWT_SECRET for safer local sharing.");
        }

        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes.");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
    }

    public String generateAccessToken(String email, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plus(jwtProperties.getAccessTokenMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(email)
                .claim(ROLE_CLAIM, role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(signingKey)
                .compact();
    }

    public String extractSubject(String token) {
        return parseClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return parseClaims(token).get(ROLE_CLAIM, String.class);
    }

    public long getExpiresInSeconds(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long seconds = (expiration.getTime() - Instant.now().toEpochMilli()) / 1000L;
        return Math.max(seconds, 0L);
    }

    public boolean isTokenValid(String token) {
        try {
            Claims claims = parseClaims(token);
            Date expiration = claims.getExpiration();
            return expiration != null && expiration.toInstant().isAfter(Instant.now());
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
