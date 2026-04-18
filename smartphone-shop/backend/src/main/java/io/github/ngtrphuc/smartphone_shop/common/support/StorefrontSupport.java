package io.github.ngtrphuc.smartphone_shop.common.support;

import java.util.Locale;

import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;

public final class StorefrontSupport {

    private StorefrontSupport() {
    }

    public static String extractBrand(String productName) {
        String normalized = normalizeInline(productName);
        if (normalized.isBlank()) {
            return "Other";
        }

        String lower = normalized.toLowerCase(Locale.ROOT);
        if (lower.startsWith("apple iphone") || lower.startsWith("iphone")) return "Apple";
        if (lower.startsWith("samsung") || lower.startsWith("galaxy")) return "Samsung";
        if (lower.startsWith("google") || lower.startsWith("pixel")) return "Google";
        if (lower.startsWith("oppo") || lower.startsWith("find ")) return "OPPO";
        if (lower.startsWith("vivo")) return "Vivo";
        if (lower.startsWith("xiaomi")) return "Xiaomi";
        if (lower.startsWith("sony") || lower.startsWith("xperia")) return "Sony";
        if (lower.startsWith("asus") || lower.startsWith("rog")) return "ASUS";
        if (lower.startsWith("zte") || lower.startsWith("nubia") || lower.startsWith("redmagic")) return "ZTE";
        if (lower.startsWith("huawei")) return "Huawei";
        if (lower.startsWith("honor")) return "Honor";
        return capitalize(normalized.split("\\s+")[0]);
    }

    public static String paymentDisplayName(PaymentMethod.Type type) {
        if (type == null) {
            return "Cash on Delivery";
        }
        return switch (type) {
            case CASH_ON_DELIVERY -> "Cash on Delivery";
            case BANK_TRANSFER -> "Bank Transfer";
            case PAYPAY -> "PayPay";
            case KOMBINI -> "Kombini";
            case VISA -> "Visa";
            case MASTERCARD -> "MasterCard";
        };
    }

    public static String paymentDisplayName(String paymentMethod, String detail) {
        String normalizedMethod = normalizeInline(paymentMethod).toUpperCase(Locale.ROOT);
        if (normalizedMethod.isBlank()) {
            return "Cash on Delivery";
        }

        try {
            PaymentMethod.Type type = PaymentMethod.Type.valueOf(normalizedMethod);
            if (type == PaymentMethod.Type.BANK_TRANSFER) {
                String maskedDetail = maskPaymentDetail(detail);
                return maskedDetail == null
                        ? paymentDisplayName(type)
                        : paymentDisplayName(type) + " - " + maskedDetail;
            }
            return paymentDisplayName(type);
        } catch (IllegalArgumentException ex) {
            return normalizeInline(paymentMethod);
        }
    }

    public static String maskPaymentDetail(String detail) {
        String normalized = normalizeInline(detail).replaceAll("\\s+", "");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() <= 4) {
            return "****";
        }
        return "****" + normalized.substring(normalized.length() - 4);
    }

    public static String orderCode(Long id) {
        if (id == null) {
            return "SPH-PENDING";
        }
        return String.format(Locale.ROOT, "SPH-%06d", id);
    }

    private static String normalizeInline(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Other";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }
}
