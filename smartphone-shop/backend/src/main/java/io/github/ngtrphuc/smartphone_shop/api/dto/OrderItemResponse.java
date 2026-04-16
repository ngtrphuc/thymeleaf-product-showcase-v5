package io.github.ngtrphuc.smartphone_shop.api.dto;

public record OrderItemResponse(
        Long productId,
        String productName,
        Double price,
        Integer quantity) {
}
