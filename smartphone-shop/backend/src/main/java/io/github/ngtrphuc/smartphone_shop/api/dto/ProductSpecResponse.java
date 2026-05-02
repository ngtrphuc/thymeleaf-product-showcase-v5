package io.github.ngtrphuc.smartphone_shop.api.dto;

public record ProductSpecResponse(
        Long id,
        String key,
        String value,
        Integer sortOrder) {
}
