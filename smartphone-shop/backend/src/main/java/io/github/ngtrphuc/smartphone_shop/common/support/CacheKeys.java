package io.github.ngtrphuc.smartphone_shop.common.support;

import java.util.Locale;

public final class CacheKeys {

    private CacheKeys() {
    }

    public static String catalog(String keyword,
            String sort,
            String brand,
            String priceRange,
            Double priceMin,
            Double priceMax,
            String batteryRange,
            Integer batteryMin,
            Integer batteryMax,
            String screenSize,
            Integer pageSize,
            int page) {
        return "catalog"
                + "|keyword=" + normalize(keyword)
                + "|sort=" + normalize(sort)
                + "|brand=" + normalize(brand)
                + "|priceRange=" + normalize(priceRange)
                + "|priceMin=" + number(priceMin)
                + "|priceMax=" + number(priceMax)
                + "|batteryRange=" + normalize(batteryRange)
                + "|batteryMin=" + integer(batteryMin)
                + "|batteryMax=" + integer(batteryMax)
                + "|screenSize=" + normalize(screenSize)
                + "|pageSize=" + integer(pageSize)
                + "|page=" + page;
    }

    private static String normalize(String value) {
        if (value == null) {
            return "-";
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? "-" : normalized;
    }

    private static String number(Double value) {
        return value == null ? "-" : value.toString();
    }

    private static String integer(Integer value) {
        return value == null ? "-" : value.toString();
    }
}
