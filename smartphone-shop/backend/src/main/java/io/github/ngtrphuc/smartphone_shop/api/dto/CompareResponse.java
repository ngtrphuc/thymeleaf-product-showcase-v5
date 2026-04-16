package io.github.ngtrphuc.smartphone_shop.api.dto;

import java.util.List;

public record CompareResponse(
        List<ProductSummary> products,
        List<Long> ids,
        int maxCompare) {
}
