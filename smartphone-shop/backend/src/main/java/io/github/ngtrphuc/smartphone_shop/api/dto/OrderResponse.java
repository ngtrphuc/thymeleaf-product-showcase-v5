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
        Double totalAmount,
        String paymentMethod,
        String paymentPlan,
        Integer installmentMonths,
        Long installmentMonthlyAmount,
        LocalDateTime createdAt,
        int itemCount,
        boolean cancelable,
        List<OrderItemResponse> items) {
}
