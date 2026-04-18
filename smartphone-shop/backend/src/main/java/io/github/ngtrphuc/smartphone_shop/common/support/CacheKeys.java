package io.github.ngtrphuc.smartphone_shop.common.support;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class CacheKeys {

    private static final char PART_SEPARATOR = '\u0000';

    private CacheKeys() {
    }

    public static String catalog(String keyword,
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
            int page) {
        String payload = Stream.of(
                "keyword=" + normalize(keyword),
                "sort=" + normalize(sort),
                "brand=" + normalize(brand),
                "storage=" + normalize(storage),
                "priceRange=" + normalize(priceRange),
                "priceMin=" + number(priceMin),
                "priceMax=" + number(priceMax),
                "batteryRange=" + normalize(batteryRange),
                "batteryMin=" + integer(batteryMin),
                "batteryMax=" + integer(batteryMax),
                "screenSize=" + normalize(screenSize),
                "pageSize=" + integer(pageSize),
                "page=" + page)
                .collect(Collectors.joining(String.valueOf(PART_SEPARATOR)));
        return "catalog:v1:" + sha256Hex(payload);
    }

    public static String productDetail(long id) {
        return "product-detail:v1:" + sha256Hex("id=" + id);
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

    private static String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required for cache key hashing.", ex);
        }
    }
}
