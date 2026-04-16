package io.github.ngtrphuc.smartphone_shop.api.dto;

public record AuthMeResponse(
        boolean authenticated,
        String email,
        String role,
        String fullName) {
}
