package io.github.ngtrphuc.smartphone_shop.api.dto;

public record ProductVariantResponse(
        Long id,
        String sku,
        String color,
        String storage,
        String ram,
        Double price,
        Integer stock,
        boolean active,
        String label,
        boolean selected) {
}
