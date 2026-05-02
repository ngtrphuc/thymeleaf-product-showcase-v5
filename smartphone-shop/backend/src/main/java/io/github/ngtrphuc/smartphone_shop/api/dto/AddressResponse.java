package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.time.LocalDateTime;

public record AddressResponse(
        Long id,
        String label,
        String recipientName,
        String phoneNumber,
        String postalCode,
        String prefecture,
        String city,
        String streetAddress,
        String building,
        boolean isDefault,
        LocalDateTime createdAt,
        String fullAddress) {
}
