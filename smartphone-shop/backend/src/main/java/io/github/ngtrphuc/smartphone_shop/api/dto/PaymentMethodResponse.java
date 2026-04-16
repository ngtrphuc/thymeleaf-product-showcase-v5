package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.time.LocalDateTime;

public record PaymentMethodResponse(
        Long id,
        String type,
        String displayName,
        String maskedDetail,
        boolean isDefault,
        boolean active,
        LocalDateTime createdAt) {
}
