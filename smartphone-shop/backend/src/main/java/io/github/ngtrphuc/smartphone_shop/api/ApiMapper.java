package io.github.ngtrphuc.smartphone_shop.api;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Component;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderItem;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.common.support.AssetUrlResolver;
import io.github.ngtrphuc.smartphone_shop.common.support.StorefrontSupport;

@Component
public class ApiMapper {

    private final AssetUrlResolver assetUrlResolver;

    public ApiMapper(AssetUrlResolver assetUrlResolver) {
        this.assetUrlResolver = assetUrlResolver;
    }

    public ProductSummary toProductSummary(Product product, boolean wishlisted) {
        if (product == null) {
            return null;
        }
        return new ProductSummary(
                product.getId(),
                product.getName(),
                StorefrontSupport.extractBrand(product.getName()),
                product.getPrice(),
                assetUrlResolver.resolve(product.getImageUrl()),
                product.getStock(),
                product.isAvailable(),
                product.isLowStock(),
                product.getAvailabilityLabel(),
                product.getMonthlyInstallmentAmount(),
                product.getStorage(),
                product.getRam(),
                product.getSize(),
                product.getOs(),
                product.getChipset(),
                product.getSpeed(),
                product.getResolution(),
                product.getBattery(),
                product.getCharging(),
                product.getDescription(),
                wishlisted);
    }

    public CartItemResponse toCartItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }
        return new CartItemResponse(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                assetUrlResolver.resolve(item.getImageUrl()),
                item.getAvailableStock(),
                item.getLineTotal(),
                item.isLowStock(),
                item.getAvailabilityLabel());
    }

    public CartResponse toCartResponse(List<CartItem> items, boolean authenticated) {
        List<CartItemResponse> mappedItems = items.stream()
                .map(this::toCartItemResponse)
                .toList();
        int itemCount = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        double totalAmount = items.stream()
                .mapToDouble(CartItem::getLineTotal)
                .sum();
        return new CartResponse(mappedItems, totalAmount, itemCount, authenticated);
    }

    public WishlistItemResponse toWishlistItemResponse(WishlistItem item) {
        if (item == null) {
            return null;
        }
        return new WishlistItemResponse(
                item.getProductId(),
                item.getName(),
                item.getPrice(),
                assetUrlResolver.resolve(item.getImageUrl()),
                item.getStock(),
                item.getAddedAt());
    }

    public PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        return new PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getType() != null ? paymentMethod.getType().name() : null,
                paymentMethod.getDisplayName(),
                paymentMethod.getMaskedDetail(),
                paymentMethod.isDefault(),
                paymentMethod.isActive(),
                paymentMethod.getCreatedAt());
    }

    public OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }
        List<OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getOrderCode(),
                order.getStatus(),
                order.getStatusSummary(),
                order.getCustomerName(),
                order.getPhoneNumber(),
                order.getShippingAddress(),
                order.getTotalAmount(),
                order.getPaymentMethodDisplayName(),
                order.getPaymentPlanDisplayName(),
                order.getInstallmentMonths(),
                order.getInstallmentMonthlyAmount(),
                order.getCreatedAt(),
                order.getItemCount(),
                order.isCancelable(),
                items);
    }

    public OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity());
    }

    public ProfileResponse toProfileResponse(User user,
            List<Order> orders,
            List<CartItem> cartItems,
            List<PaymentMethod> paymentMethods) {
        long deliveredOrderCount = orders.stream()
                .filter(order -> "delivered".equals(order.getStatus()))
                .count();
        long pendingOrderCount = orders.stream()
                .filter(order -> !"delivered".equals(order.getStatus()) && !"cancelled".equals(order.getStatus()))
                .count();
        int cartItemCount = cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        return toProfileResponse(user, deliveredOrderCount, pendingOrderCount, cartItemCount, paymentMethods);
    }

    public ProfileResponse toProfileResponse(User user,
            long deliveredOrderCount,
            long pendingOrderCount,
            int cartItemCount,
            List<PaymentMethod> paymentMethods) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.getDefaultAddress(),
                deliveredOrderCount,
                pendingOrderCount,
                cartItemCount,
                paymentMethods.stream().map(this::toPaymentMethodResponse).toList());
    }

    public AuthMeResponse toAuthMeResponse(User user) {
        if (user == null) {
            return new AuthMeResponse(false, null, null, null);
        }
        return new AuthMeResponse(true, user.getEmail(), normalizeRole(user.getRole()), user.getFullName());
    }

    public ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return new ChatMessageResponse(
                message.getId(),
                message.getUserEmail(),
                message.getContent(),
                message.getSenderRole(),
                message.getCreatedAt());
    }

    private String normalizeRole(String role) {
        String normalized = role == null ? "" : role.trim().toUpperCase(Locale.ROOT);
        if (normalized.isBlank()) {
            return "ROLE_USER";
        }
        if (normalized.startsWith("ROLE_")) {
            return normalized;
        }
        return "ROLE_" + normalized;
    }
}


