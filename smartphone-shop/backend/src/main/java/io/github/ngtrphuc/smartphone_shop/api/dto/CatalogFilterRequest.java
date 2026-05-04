package io.github.ngtrphuc.smartphone_shop.api.dto;

public record CatalogFilterRequest(
        String keyword,
        String sort,
        String brand,
        String storage,
        String priceRange,
        Double priceMin,
        Double priceMax,
        String batteryRange,
        Integer batteryMin,
        Integer batteryMax,
        String screenSize,
        Integer pageSize,
        Integer page) {

    public int resolvedPage() {
        return page == null ? 0 : page;
    }
}
