package io.github.ngtrphuc.smartphone_shop.model;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public enum OrderStatus {
    PENDING("pending"),
    PROCESSING("processing"),
    SHIPPED("shipped"),
    DELIVERED("delivered"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    RETURN_REQUESTED("return_requested"),
    RETURN_APPROVED("return_approved"),
    RETURN_REJECTED("return_rejected"),
    REFUNDED("refunded");

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
            PENDING, EnumSet.of(PROCESSING, CANCELLED),
            PROCESSING, EnumSet.of(SHIPPED, CANCELLED),
            SHIPPED, EnumSet.of(DELIVERED),
            DELIVERED, EnumSet.of(COMPLETED, RETURN_REQUESTED),
            COMPLETED, EnumSet.noneOf(OrderStatus.class),
            CANCELLED, EnumSet.of(PENDING),
            RETURN_REQUESTED, EnumSet.of(RETURN_APPROVED, RETURN_REJECTED),
            RETURN_APPROVED, EnumSet.of(REFUNDED),
            RETURN_REJECTED, EnumSet.noneOf(OrderStatus.class),
            REFUNDED, EnumSet.noneOf(OrderStatus.class));

    private final String value;

    OrderStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public boolean canTransitionTo(OrderStatus target) {
        if (target == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(this, Set.of()).contains(target);
    }

    public boolean isCancelableByCustomer() {
        return this == PENDING || this == PROCESSING;
    }

    public static OrderStatus from(String rawStatus) {
        String normalized = rawStatus == null ? "" : rawStatus.trim().toLowerCase(Locale.ROOT);
        for (OrderStatus status : values()) {
            if (status.value.equals(normalized)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unsupported order status: " + rawStatus);
    }
}
