package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.io.Serializable;

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
        String os,
        String chipset,
        String speed,
        String resolution,
        String battery,
        String charging,
        String description,
        boolean wishlisted,
        Long categoryId,
        String categoryName,
        String slug,
        String skuPrefix,
        Long defaultVariantId,
        String defaultVariantLabel) implements Serializable {

    private static final long serialVersionUID = 1L;
}
