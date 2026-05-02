package io.github.ngtrphuc.smartphone_shop.event;

public record OrderStatusChangedEvent(
        Long orderId,
        String orderCode,
        String userEmail,
        String oldStatus,
        String newStatus,
        String trackingNumber,
        String trackingCarrier) {
}
