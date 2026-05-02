package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderCode,
        String status,
        String statusSummary,
        String customerName,
        String phoneNumber,
        String shippingAddress,
        String trackingNumber,
        String trackingCarrier,
        Double totalAmount,
        String paymentMethod,
        String paymentPlan,
        Integer installmentMonths,
        Long installmentMonthlyAmount,
        LocalDateTime createdAt,
        LocalDateTime shippedAt,
        LocalDateTime deliveredAt,
        LocalDateTime completedAt,
        int itemCount,
        boolean cancelable,
        List<OrderItemResponse> items) {
}
