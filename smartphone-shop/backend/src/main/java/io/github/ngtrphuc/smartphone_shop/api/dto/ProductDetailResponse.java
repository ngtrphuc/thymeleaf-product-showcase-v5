package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.util.List;

public record ProductDetailResponse(
        ProductSummary product,
        List<ProductSummary> recommendedProducts,
        boolean wishlisted) {
}
