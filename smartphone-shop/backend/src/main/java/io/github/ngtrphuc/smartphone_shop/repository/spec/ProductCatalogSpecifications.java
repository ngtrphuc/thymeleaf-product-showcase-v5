package io.github.ngtrphuc.smartphone_shop.repository.spec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.lang.Nullable;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public final class ProductCatalogSpecifications {

    private ProductCatalogSpecifications() {
    }

    public static Specification<Product> forCatalog(
            @Nullable String keyword,
            @Nullable Collection<Long> candidateIds,
            @Nullable Double priceMin,
            @Nullable Double priceMax,
            @Nullable String brand,
            @Nullable String storage,
            @Nullable String batteryRange,
            @Nullable Integer batteryMin,
            @Nullable Integer batteryMax,
            @Nullable String screenSize) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (candidateIds != null) {
                if (candidateIds.isEmpty()) {
                    return cb.disjunction();
                }
                predicates.add(root.get("id").in(candidateIds));
            }

            if (keyword != null && !keyword.isBlank()) {
                predicates.add(cb.like(
                        cb.lower(cb.coalesce(root.get("name"), "")),
                        "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%"));
            }
            if (priceMin != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), priceMin));
            }
            if (priceMax != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), priceMax));
            }

            if (brand != null && !brand.isBlank()) {
                predicates.add(buildBrandPredicate(root, cb, brand.trim().toLowerCase(Locale.ROOT)));
            }

            if (storage != null && !storage.isBlank()) {
                predicates.add(buildStoragePredicate(root, cb, storage.trim().toLowerCase(Locale.ROOT)));
            }

            if ((batteryRange != null && !batteryRange.isBlank()) || batteryMin != null || batteryMax != null) {
                Expression<String> batteryValue = batteryComparableValue(root, cb);
                if (batteryRange != null && !batteryRange.isBlank()) {
                    predicates.add(buildBatteryRangePredicate(cb, batteryValue, batteryRange.trim()));
                }
                if (batteryMin != null) {
                    predicates.add(cb.greaterThanOrEqualTo(batteryValue, paddedNumber(batteryMin)));
                }
                if (batteryMax != null) {
                    predicates.add(cb.lessThanOrEqualTo(batteryValue, paddedNumber(batteryMax)));
                }
            }

            if (screenSize != null && !screenSize.isBlank()) {
                predicates.add(buildScreenSizePredicate(root, cb, screenSize.trim()));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Predicate buildStoragePredicate(Root<Product> root, CriteriaBuilder cb, String storage) {
        Expression<String> storageLower = cb.lower(cb.coalesce(root.get("storage"), ""));
        return switch (storage) {
            case "1tb" -> cb.or(like(cb, storageLower, "%1tb%"), like(cb, storageLower, "%1 tb%"));
            case "over1tb" -> cb.or(
                    like(cb, storageLower, "%2tb%"),
                    like(cb, storageLower, "%2 tb%"),
                    like(cb, storageLower, "%3tb%"),
                    like(cb, storageLower, "%3 tb%"),
                    like(cb, storageLower, "%4tb%"),
                    like(cb, storageLower, "%4 tb%"),
                    like(cb, storageLower, "%1536gb%"),
                    like(cb, storageLower, "%2048gb%"));
            default -> like(cb, storageLower, "%" + storage + "%");
        };
    }

    private static Predicate buildBrandPredicate(Root<Product> root, CriteriaBuilder cb, String brand) {
        Expression<String> nameLower = cb.lower(cb.coalesce(root.get("name"), ""));

        return switch (brand) {
            case "apple" -> cb.or(like(cb, nameLower, "apple iphone%"), like(cb, nameLower, "iphone%"));
            case "samsung" -> cb.or(like(cb, nameLower, "samsung%"), like(cb, nameLower, "galaxy%"));
            case "google" -> cb.or(like(cb, nameLower, "google%"), like(cb, nameLower, "pixel%"));
            case "oppo" -> cb.or(like(cb, nameLower, "oppo%"), like(cb, nameLower, "find %"));
            case "vivo" -> like(cb, nameLower, "vivo%");
            case "xiaomi" -> like(cb, nameLower, "xiaomi%");
            case "sony" -> cb.or(like(cb, nameLower, "sony%"), like(cb, nameLower, "xperia%"));
            case "asus" -> cb.or(like(cb, nameLower, "asus%"), like(cb, nameLower, "rog%"));
            case "zte" -> cb.or(like(cb, nameLower, "zte%"), like(cb, nameLower, "nubia%"), like(cb, nameLower, "redmagic%"));
            case "huawei" -> like(cb, nameLower, "huawei%");
            case "honor" -> like(cb, nameLower, "honor%");
            case "other" -> cb.not(cb.or(
                    like(cb, nameLower, "apple iphone%"),
                    like(cb, nameLower, "iphone%"),
                    like(cb, nameLower, "samsung%"),
                    like(cb, nameLower, "galaxy%"),
                    like(cb, nameLower, "google%"),
                    like(cb, nameLower, "pixel%"),
                    like(cb, nameLower, "oppo%"),
                    like(cb, nameLower, "find %"),
                    like(cb, nameLower, "vivo%"),
                    like(cb, nameLower, "xiaomi%"),
                    like(cb, nameLower, "sony%"),
                    like(cb, nameLower, "xperia%"),
                    like(cb, nameLower, "asus%"),
                    like(cb, nameLower, "rog%"),
                    like(cb, nameLower, "zte%"),
                    like(cb, nameLower, "nubia%"),
                    like(cb, nameLower, "redmagic%"),
                    like(cb, nameLower, "huawei%"),
                    like(cb, nameLower, "honor%")));
            default -> like(cb, nameLower, brand + "%");
        };
    }

    private static Predicate buildScreenSizePredicate(Root<Product> root, CriteriaBuilder cb, String screenSize) {
        Expression<String> sizeLower = cb.lower(cb.coalesce(root.get("size"), ""));
        return switch (screenSize) {
            case "6.1to6.3" -> cb.or(
                    like(cb, sizeLower, "6.1%"),
                    like(cb, sizeLower, "6.2%"),
                    like(cb, sizeLower, "6.3%"));
            case "6.4to6.6" -> cb.or(
                    like(cb, sizeLower, "6.4%"),
                    like(cb, sizeLower, "6.5%"),
                    like(cb, sizeLower, "6.6%"));
            case "6.7to6.9" -> cb.or(
                    like(cb, sizeLower, "6.7%"),
                    like(cb, sizeLower, "6.8%"),
                    like(cb, sizeLower, "6.9%"));
            case "over6.6" -> cb.or(
                    like(cb, sizeLower, "6.7%"),
                    like(cb, sizeLower, "6.8%"),
                    like(cb, sizeLower, "6.9%"),
                    like(cb, sizeLower, "7%"),
                    like(cb, sizeLower, "8%"),
                    like(cb, sizeLower, "9%"));
            case "over7.0" -> cb.or(
                    like(cb, sizeLower, "7%"),
                    like(cb, sizeLower, "8%"),
                    like(cb, sizeLower, "9%"));
            case "7.0to7.9" -> like(cb, sizeLower, "7%");
            case "8.0plus" -> cb.or(
                    like(cb, sizeLower, "8%"),
                    like(cb, sizeLower, "9%"));
            case "under6.1" -> cb.or(
                    like(cb, sizeLower, "0%"),
                    like(cb, sizeLower, "1%"),
                    like(cb, sizeLower, "2%"),
                    like(cb, sizeLower, "3%"),
                    like(cb, sizeLower, "4%"),
                    like(cb, sizeLower, "5%"),
                    like(cb, sizeLower, "6.0%"));
            case "6.1to6.4" -> cb.or(
                    like(cb, sizeLower, "6.1%"),
                    like(cb, sizeLower, "6.2%"),
                    like(cb, sizeLower, "6.3%"),
                    like(cb, sizeLower, "6.4%"));
            case "6.5to6.6" -> cb.or(
                    like(cb, sizeLower, "6.5%"),
                    like(cb, sizeLower, "6.6%"));
            case "6.7to6.8" -> cb.or(
                    like(cb, sizeLower, "6.7%"),
                    like(cb, sizeLower, "6.8%"));
            case "under6.5" -> cb.or(
                    like(cb, sizeLower, "0%"),
                    like(cb, sizeLower, "1%"),
                    like(cb, sizeLower, "2%"),
                    like(cb, sizeLower, "3%"),
                    like(cb, sizeLower, "4%"),
                    like(cb, sizeLower, "5%"),
                    like(cb, sizeLower, "6.0%"),
                    like(cb, sizeLower, "6.1%"),
                    like(cb, sizeLower, "6.2%"),
                    like(cb, sizeLower, "6.3%"),
                    like(cb, sizeLower, "6.4%"));
            case "6.5to6.8" -> cb.or(
                    like(cb, sizeLower, "6.5%"),
                    like(cb, sizeLower, "6.6%"),
                    like(cb, sizeLower, "6.7%"),
                    like(cb, sizeLower, "6.8%"));
            case "over6.8" -> cb.or(
                    like(cb, sizeLower, "6.9%"),
                    like(cb, sizeLower, "7%"),
                    like(cb, sizeLower, "8%"),
                    like(cb, sizeLower, "9%"));
            default -> cb.conjunction();
        };
    }

    private static Predicate buildBatteryRangePredicate(CriteriaBuilder cb, Expression<String> batteryValue, String batteryRange) {
        return switch (batteryRange) {
            case "under3500" -> cb.lessThan(batteryValue, paddedNumber(3500));
            case "3500to3999" -> cb.and(
                    cb.greaterThanOrEqualTo(batteryValue, paddedNumber(3500)),
                    cb.lessThanOrEqualTo(batteryValue, paddedNumber(3999)));
            case "under4000" -> cb.lessThan(batteryValue, paddedNumber(4000));
            case "4000to4499" -> cb.and(
                    cb.greaterThanOrEqualTo(batteryValue, paddedNumber(4000)),
                    cb.lessThanOrEqualTo(batteryValue, paddedNumber(4499)));
            case "4500to4999" -> cb.and(
                    cb.greaterThanOrEqualTo(batteryValue, paddedNumber(4500)),
                    cb.lessThanOrEqualTo(batteryValue, paddedNumber(4999)));
            case "5000to5499" -> cb.and(
                    cb.greaterThanOrEqualTo(batteryValue, paddedNumber(5000)),
                    cb.lessThanOrEqualTo(batteryValue, paddedNumber(5499)));
            case "5500to5999" -> cb.and(
                    cb.greaterThanOrEqualTo(batteryValue, paddedNumber(5500)),
                    cb.lessThanOrEqualTo(batteryValue, paddedNumber(5999)));
            case "6000to6999" -> cb.and(
                    cb.greaterThanOrEqualTo(batteryValue, paddedNumber(6000)),
                    cb.lessThanOrEqualTo(batteryValue, paddedNumber(6999)));
            case "over7000" -> cb.greaterThanOrEqualTo(batteryValue, paddedNumber(7000));
            case "over5500" -> cb.greaterThanOrEqualTo(batteryValue, paddedNumber(5500));
            case "under5000" -> cb.lessThan(batteryValue, paddedNumber(5000));
            case "over5000" -> cb.greaterThanOrEqualTo(batteryValue, paddedNumber(5000));
            default -> cb.conjunction();
        };
    }

    private static Expression<String> batteryComparableValue(Root<Product> root, CriteriaBuilder cb) {
        Expression<String> normalized = cb.lower(cb.coalesce(root.get("battery"), ""));
        normalized = cb.function("replace", String.class, normalized, cb.literal("mah"), cb.literal(""));
        normalized = cb.function("replace", String.class, normalized, cb.literal(" "), cb.literal(""));
        normalized = cb.function("replace", String.class, normalized, cb.literal(","), cb.literal(""));
        normalized = cb.function("replace", String.class, normalized, cb.literal("."), cb.literal(""));
        normalized = cb.function("replace", String.class, normalized, cb.literal("+"), cb.literal(""));
        normalized = cb.function("replace", String.class, normalized, cb.literal("-"), cb.literal(""));

        Expression<String> fallback = cb.<String>selectCase()
                .when(cb.equal(normalized, ""), "0")
                .otherwise(normalized);
        return cb.function("lpad", String.class, fallback, cb.literal(8), cb.literal("0"));
    }

    private static String paddedNumber(int value) {
        int safeValue = Math.max(0, value);
        return String.format(Locale.ROOT, "%08d", safeValue);
    }

    private static Predicate like(CriteriaBuilder cb, Expression<String> expression, String pattern) {
        return cb.like(expression, pattern);
    }
}
