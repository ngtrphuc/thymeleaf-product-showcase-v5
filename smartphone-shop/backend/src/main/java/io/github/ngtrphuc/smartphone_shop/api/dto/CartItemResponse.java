package io.github.ngtrphuc.smartphone_shop.api.dto;

public record CartItemResponse(
        Long id,
        String name,
        Double price,
        int quantity,
        String imageUrl,
        int availableStock,
        double lineTotal,
        boolean lowStock,
        String availabilityLabel,
        Long variantId,
        String variantSku,
        String variantLabel) {
}
