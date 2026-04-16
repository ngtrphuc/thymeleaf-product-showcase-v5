package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        Long id,
        String userEmail,
        String content,
        String senderRole,
        LocalDateTime createdAt) {
}
