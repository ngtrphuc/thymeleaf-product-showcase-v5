package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.util.List;

public record CatalogPageResponse(
        List<ProductSummary> products,
        int currentPage,
        int totalPages,
        long totalElements,
        int pageSize,
        List<String> brands,
        int activeFilterCount,
        boolean hasActiveFilters) {
}
