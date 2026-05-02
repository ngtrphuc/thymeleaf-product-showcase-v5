package io.github.ngtrphuc.smartphone_shop.api.dto;

public record ProductImageResponse(
        Long id,
        String url,
        Integer sortOrder,
        boolean primary) {
}
