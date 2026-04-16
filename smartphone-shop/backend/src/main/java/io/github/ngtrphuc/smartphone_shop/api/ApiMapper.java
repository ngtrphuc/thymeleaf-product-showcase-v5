package io.github.ngtrphuc.smartphone_shop.api;

import java.util.List;

import org.springframework.stereotype.Component;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderItem;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.support.StorefrontSupport;

@Component
public class ApiMapper {

    public ApiDtos.ProductSummary toProductSummary(Product product, boolean wishlisted) {
        if (product == null) {
            return null;
        }
        return new ApiDtos.ProductSummary(
                product.getId(),
                product.getName(),
                StorefrontSupport.extractBrand(product.getName()),
                product.getPrice(),
                product.getImageUrl(),
                product.getStock(),
                product.isAvailable(),
                product.isLowStock(),
                product.getAvailabilityLabel(),
                product.getMonthlyInstallmentAmount(),
                product.getStorage(),
                product.getRam(),
                product.getSize(),
                wishlisted);
    }

    public ApiDtos.CartItemResponse toCartItemResponse(CartItem item) {
        if (item == null) {
            return null;
        }
        return new ApiDtos.CartItemResponse(
                item.getId(),
                item.getName(),
                item.getPrice(),
                item.getQuantity(),
                item.getImageUrl(),
                item.getAvailableStock(),
                item.getLineTotal(),
                item.isLowStock(),
                item.getAvailabilityLabel());
    }

    public ApiDtos.CartResponse toCartResponse(List<CartItem> items, boolean authenticated) {
        List<ApiDtos.CartItemResponse> mappedItems = items.stream()
                .map(this::toCartItemResponse)
                .toList();
        int itemCount = items.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
        double totalAmount = items.stream()
                .mapToDouble(CartItem::getLineTotal)
                .sum();
        return new ApiDtos.CartResponse(mappedItems, totalAmount, itemCount, authenticated);
    }

    public ApiDtos.WishlistItemResponse toWishlistItemResponse(WishlistItem item) {
        if (item == null) {
            return null;
        }
        return new ApiDtos.WishlistItemResponse(
                item.getProductId(),
                item.getName(),
                item.getPrice(),
                item.getImageUrl(),
                item.getStock(),
                item.getAddedAt());
    }

    public ApiDtos.PaymentMethodResponse toPaymentMethodResponse(PaymentMethod paymentMethod) {
        if (paymentMethod == null) {
            return null;
        }
        return new ApiDtos.PaymentMethodResponse(
                paymentMethod.getId(),
                paymentMethod.getType() != null ? paymentMethod.getType().name() : null,
                paymentMethod.getDisplayName(),
                paymentMethod.getMaskedDetail(),
                paymentMethod.isDefault(),
                paymentMethod.isActive(),
                paymentMethod.getCreatedAt());
    }

    public ApiDtos.OrderResponse toOrderResponse(Order order) {
        if (order == null) {
            return null;
        }
        List<ApiDtos.OrderItemResponse> items = order.getItems().stream()
                .map(this::toOrderItemResponse)
                .toList();
        return new ApiDtos.OrderResponse(
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

    public ApiDtos.OrderItemResponse toOrderItemResponse(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new ApiDtos.OrderItemResponse(
                item.getProductId(),
                item.getProductName(),
                item.getPrice(),
                item.getQuantity());
    }

    public ApiDtos.ProfileResponse toProfileResponse(User user,
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
        return new ApiDtos.ProfileResponse(
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

    public ApiDtos.AuthMeResponse toAuthMeResponse(User user) {
        if (user == null) {
            return new ApiDtos.AuthMeResponse(false, null, null, null);
        }
        return new ApiDtos.AuthMeResponse(true, user.getEmail(), user.getRole(), user.getFullName());
    }

    public ApiDtos.ChatMessageResponse toChatMessageResponse(ChatMessage message) {
        if (message == null) {
            return null;
        }
        return new ApiDtos.ChatMessageResponse(
                message.getId(),
                message.getUserEmail(),
                message.getContent(),
                message.getSenderRole(),
                message.getCreatedAt());
    }
}
