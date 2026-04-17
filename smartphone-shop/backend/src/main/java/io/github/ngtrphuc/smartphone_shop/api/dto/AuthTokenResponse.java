package io.github.ngtrphuc.smartphone_shop.api.dto;

public record AuthTokenResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        String email,
        String role,
        String fullName) {
}
