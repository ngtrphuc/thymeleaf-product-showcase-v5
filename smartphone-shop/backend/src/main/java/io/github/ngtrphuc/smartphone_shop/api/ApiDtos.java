package io.github.ngtrphuc.smartphone_shop.api;

import java.time.LocalDateTime;
import java.util.List;

public final class ApiDtos {

    private ApiDtos() {
    }

    public record ErrorResponse(String code, String message) {
    }

    public record OperationStatusResponse(boolean success, String message) {
    }

    public record ProductSummary(
            Long id,
            String name,
            String brand,
            Double price,
            String imageUrl,
            Integer stock,
            boolean available,
            boolean lowStock,
            String availabilityLabel,
            long monthlyInstallmentAmount,
            String storage,
            String ram,
            String size,
            boolean wishlisted) {
    }

    public record CatalogPageResponse(
            List<ProductSummary> products,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            List<String> brands,
            int activeFilterCount,
            boolean hasActiveFilters) {
    }

    public record ProductDetailResponse(
            ProductSummary product,
            List<ProductSummary> recommendedProducts,
            boolean wishlisted) {
    }

    public record CartItemResponse(
            Long id,
            String name,
            Double price,
            int quantity,
            String imageUrl,
            int availableStock,
            double lineTotal,
            boolean lowStock,
            String availabilityLabel) {
    }

    public record CartResponse(
            List<CartItemResponse> items,
            double totalAmount,
            int itemCount,
            boolean authenticated) {
    }

    public record CompareResponse(
            List<ProductSummary> products,
            List<Long> ids,
            int maxCompare) {
    }

    public record WishlistItemResponse(
            Long productId,
            String name,
            Double price,
            String imageUrl,
            Integer stock,
            LocalDateTime addedAt) {
    }

    public record WishlistResponse(
            List<WishlistItemResponse> items,
            long count) {
    }

    public record PaymentMethodResponse(
            Long id,
            String type,
            String displayName,
            String maskedDetail,
            boolean isDefault,
            boolean active,
            LocalDateTime createdAt) {
    }

    public record OrderItemResponse(
            Long productId,
            String productName,
            Double price,
            Integer quantity) {
    }

    public record OrderResponse(
            Long id,
            String orderCode,
            String status,
            String statusSummary,
            String customerName,
            String phoneNumber,
            String shippingAddress,
            Double totalAmount,
            String paymentMethod,
            String paymentPlan,
            Integer installmentMonths,
            Long installmentMonthlyAmount,
            LocalDateTime createdAt,
            int itemCount,
            boolean cancelable,
            List<OrderItemResponse> items) {
    }

    public record ProfileResponse(
            Long id,
            String email,
            String fullName,
            String phoneNumber,
            String defaultAddress,
            long deliveredOrderCount,
            long pendingOrderCount,
            int cartItemCount,
            List<PaymentMethodResponse> paymentMethods) {
    }

    public record AuthMeResponse(
            boolean authenticated,
            String email,
            String role,
            String fullName) {
    }

    public record ChatMessageResponse(
            Long id,
            String userEmail,
            String content,
            String senderRole,
            LocalDateTime createdAt) {
    }
}
