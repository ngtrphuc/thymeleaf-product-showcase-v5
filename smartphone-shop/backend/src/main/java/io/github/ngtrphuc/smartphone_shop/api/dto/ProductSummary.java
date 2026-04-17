package io.github.ngtrphuc.smartphone_shop.api.dto;

public record ProductSummary(
        Long id,
        String name,
        String brand,
        Double price,
        String imageUrl,
        Integer stock,
        boolean available,
        boolean lowStock,
        String availabilityLabel,
        long monthlyInstallmentAmount,
        String storage,
        String ram,
        String size,
        boolean wishlisted) {
}
