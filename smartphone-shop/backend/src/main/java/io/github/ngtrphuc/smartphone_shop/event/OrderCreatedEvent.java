package io.github.ngtrphuc.smartphone_shop.event;

import java.time.LocalDateTime;

public record OrderCreatedEvent(
        Long orderId,
        String userEmail,
        String orderCode,
        Double totalAmount,
        int itemCount,
        String paymentMethod,
        String paymentPlan,
        LocalDateTime createdAt) {
}
