package io.github.ngtrphuc.smartphone_shop.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.github.ngtrphuc.smartphone_shop.api.dto.AuthMeResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.AddressResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.CartItemResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.CartResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ChatMessageResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OrderItemResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OrderResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.PaymentMethodResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductImageResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductSpecResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductSummary;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductVariantResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProfileResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.WishlistItemResponse;
import io.github.ngtrphuc.smartphone_shop.common.support.AssetUrlResolver;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.ChatMessage;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderItem;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.model.Address;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.ProductImage;
import io.github.ngtrphuc.smartphone_shop.model.ProductSpec;
import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import io.github.ngtrphuc.smartphone_shop.model.User;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.service.ProductCommerceService;

@Component
public class ApiMapper {

    private final AssetUrlResolver assetUrlResolver;
    private final ProductCommerceService productCommerceService;

    @Autowired
    public ApiMapper(AssetUrlResolver assetUrlResolver,
            ProductCommerceService productCommerceService) {
        this.assetUrlResolver = assetUrlResolver;
        this.productCommerceService = productCommerceService;
    }

    /**
     * Backward-compatible constructor for tests and lightweight callers.
     */
    public ApiMapper(AssetUrlResolver assetUrlResolver) {
        this(assetUrlResolver, new ProductCommerceService(null, null, null, null, null));
    }

    public ProductSummary toProductSummary(Product product, boolean wishlisted) {
        if (product == null) {
            return null;
        }
        ProductVariant defaultVariant = productCommerceService.resolveVariantOrDefault(product, null);
        List<ProductImage> images = productCommerceService.loadImages(product.getId());
        String imageUrl = productCommerceService.resolvePrimaryImageUrl(product, images);
        double price = productCommerceService.resolveEffectivePrice(product, defaultVariant);
        int stock = productCommerceService.resolveEffectiveStock(product, defaultVariant);
        String storage = defaultVariant != null && defaultVariant.getStorage() != null && !defaultVariant.getStorage().isBlank()
                ? defaultVariant.getStorage()
                : product.getStorage();
        String ram = defaultVariant != null && defaultVariant.getRam() != null && !defaultVariant.getRam().isBlank()
                ? defaultVariant.getRam()
                : product.getRam();
        String variantLabel = defaultVariant != null ? productCommerceService.resolveVariantLabel(defaultVariant) : null;

        return new ProductSummary(
                product.getId(),
                product.getName(),
                product.getBrandNameOrFallback(),
                price,
                assetUrlResolver.resolve(imageUrl),
                stock,
                stock > 0,
                stock > 0 && stock <= 3,
                stock <= 0 ? "Out of stock" : (stock <= 3 ? "Only " + stock + " left" : "In stock"),
                price > 0 ? Math.round(price / 24.0) : 0L,
                storage,
                ram,
                product.getSize(),
                product.getOs(),
                product.getChipset(),
                product.getSpeed(),
                product.getResolution(),
                product.getBattery(),
                product.getCharging(),
                product.getDescription(),
                wishlisted,
                product.getCategory() != null ? product.getCategory().getId() : null,
                product.getCategory() != null ? product.getCategory().getName() : null,
                product.getSlug(),
                product.getSkuPrefix(),
                defaultVariant != null ? defaultVariant.getId() : null,
                variantLabel);
    }

    public ProductVariantResponse toProductVariantResponse(Product product, ProductVariant variant, Long selectedVariantId) {
        if (variant == null) {
            return null;
        }
        double resolvedPrice = productCommerceService.resolveEffectivePrice(product, variant);
        String label = productCommerceService.resolveVariantLabel(variant);
        return new ProductVariantResponse(
                variant.getId(),
                variant.getSku(),
                variant.getColor(),
                variant.getStorage(),
                variant.getRam(),
                resolvedPrice,
                variant.getStock(),
                variant.isActive(),
                label,
                selectedVariantId != null && selectedVariantId.equals(variant.getId()));
    }

    public ProductImageResponse toProductImageResponse(ProductImage image) {
        if (image == null) {
            return null;
        }
        return new ProductImageResponse(
                image.getId(),
                assetUrlResolver.resolve(image.getUrl()),
                image.getSortOrder(),
                image.isPrimary());
    }

    public ProductSpecResponse toProductSpecResponse(ProductSpec spec) {
        if (spec == null) {
            return null;
        }
        return new ProductSpecResponse(spec.getId(), spec.getSpecKey(), spec.getSpecValue(), spec.getSortOrder());
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
                item.getAvailabilityLabel(),
                item.getVariantId(),
                item.getVariantSku(),
                item.getVariantLabel());
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
                order.getTrackingNumber(),
                order.getTrackingCarrier(),
                order.getTotalAmount(),
                order.getPaymentMethodDisplayName(),
                order.getPaymentPlanDisplayName(),
                order.getInstallmentMonths(),
                order.getInstallmentMonthlyAmount(),
                order.getCreatedAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                order.getCompletedAt(),
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
                item.getQuantity(),
                item.getVariantId(),
                item.getVariantSku(),
                item.getVariantLabel());
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

    public AddressResponse toAddressResponse(Address address) {
        if (address == null) {
            return null;
        }
        return new AddressResponse(
                address.getId(),
                address.getLabel(),
                address.getRecipientName(),
                address.getPhoneNumber(),
                address.getPostalCode(),
                address.getPrefecture(),
                address.getCity(),
                address.getStreetAddress(),
                address.getBuilding(),
                address.isDefault(),
                address.getCreatedAt(),
                address.toFullAddress());
    }

    public AuthMeResponse toAuthMeResponse(User user) {
        if (user == null) {
            return new AuthMeResponse(false, null, null, null);
        }
        return new AuthMeResponse(true, user.getEmail(), user.getRoleName(), user.getFullName());
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
}
