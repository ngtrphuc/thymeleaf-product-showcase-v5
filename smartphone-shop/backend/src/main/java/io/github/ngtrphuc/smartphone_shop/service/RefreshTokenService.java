package io.github.ngtrphuc.smartphone_shop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.model.RefreshToken;
import io.github.ngtrphuc.smartphone_shop.repository.RefreshTokenRepository;
import io.github.ngtrphuc.smartphone_shop.security.JwtProperties;

@Service
public class RefreshTokenService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository, JwtProperties jwtProperties) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtProperties = jwtProperties;
    }

    @Transactional
    public IssuedRefreshToken issue(String userEmail, String userAgent) {
        String rawToken = generateRawToken();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(Math.max(1L, jwtProperties.getRefreshTokenDays()));

        RefreshToken token = new RefreshToken();
        token.setUserEmail(userEmail);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(expiresAt);
        token.setDeviceInfo(truncate(userAgent, 255));
        refreshTokenRepository.save(token);

        return new IssuedRefreshToken(rawToken, Duration.between(now, expiresAt).toSeconds());
    }

    @Transactional
    public Optional<RefreshToken> validateAndRevoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return Optional.empty();
        }
        LocalDateTime now = LocalDateTime.now();
        return refreshTokenRepository.findByTokenHash(hash(rawToken.trim()))
                .filter(token -> token.isUsable(now))
                .map(token -> {
                    token.setRevoked(true);
                    return token;
                });
    }

    @Transactional
    public void revoke(String rawToken) {
        if (rawToken == null || rawToken.isBlank()) {
            return;
        }
        refreshTokenRepository.revokeByTokenHash(hash(rawToken.trim()));
    }

    @Transactional
    public void revokeAllForUser(String userEmail) {
        if (userEmail == null || userEmail.isBlank()) {
            return;
        }
        refreshTokenRepository.revokeAllByUserEmail(userEmail.trim());
    }

    @Scheduled(cron = "${app.jwt.refresh-token-cleanup-cron:0 18 3 * * *}")
    @Transactional
    public void cleanupExpiredAndRevokedTokens() {
        refreshTokenRepository.deleteExpiredOrRevokedBefore(LocalDateTime.now());
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available.", ex);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    public record IssuedRefreshToken(String token, long expiresInSeconds) {
    }
}
