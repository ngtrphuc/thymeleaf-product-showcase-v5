package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.util.List;

public record CartResponse(
        List<CartItemResponse> items,
        double totalAmount,
        int itemCount,
        boolean authenticated) {
}
