package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.io.Serializable;
import java.util.List;

public record ProductDetailResponse(
        ProductSummary product,
        List<ProductSummary> recommendedProducts,
        boolean wishlisted,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        List<ProductSpecResponse> specs,
        Long selectedVariantId) implements Serializable {

    private static final long serialVersionUID = 1L;
}
