package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.util.List;

public record ProfileResponse(
        Long id,
        String email,
        String fullName,
        String phoneNumber,
        String defaultAddress,
        long deliveredOrderCount,
        long pendingOrderCount,
        int cartItemCount,
        List<PaymentMethodResponse> paymentMethods) {
}
