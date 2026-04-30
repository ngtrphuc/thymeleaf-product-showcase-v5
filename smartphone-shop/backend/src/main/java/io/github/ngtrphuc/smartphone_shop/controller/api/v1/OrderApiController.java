package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderIdempotencyService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final CartService cartService;
    private final OrderIdempotencyService orderIdempotencyService;
    private final ApiMapper apiMapper;

    public OrderApiController(OrderService orderService,
            CartService cartService,
            OrderIdempotencyService orderIdempotencyService,
            ApiMapper apiMapper) {
        this.orderService = orderService;
        this.cartService = cartService;
        this.orderIdempotencyService = orderIdempotencyService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public UserOrderPageResponse orders(
            Authentication authentication,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        String userEmail = authentication.getName();
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(pageSize, 50));

        long totalOrders = orderService.countOrdersByUser(userEmail);
        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / safeSize);
        int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(safePage, totalPages - 1));

        List<OrderResponse> orders = orderService.getOrdersByUser(userEmail, currentPage, safeSize).stream()
                .map(apiMapper::toOrderResponse)
                .toList();
        return new UserOrderPageResponse(orders, currentPage, totalPages, totalOrders, safeSize);
    }

    @PostMapping("/{id}/cancel")
    public OperationStatusResponse cancel(@PathVariable(name = "id") Long id, Authentication authentication) {
        boolean success = orderService.cancelOrder(id, authentication.getName());
        return new OperationStatusResponse(
                success,
                success ? "Order cancelled successfully." : "Cannot cancel this order.");
    }

    @PostMapping
    public OrderResponse place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody PlaceOrderRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        String requestFingerprint = orderIdempotencyService.createFingerprint(
                userEmail,
                request.customerName(),
                request.phoneNumber(),
                request.shippingAddress(),
                request.paymentMethod(),
                request.paymentDetail(),
                request.paymentPlan(),
                request.installmentMonths());
        Order created = orderIdempotencyService.executeCheckout(
                userEmail,
                idempotencyKey,
                requestFingerprint,
                () -> {
                    Order order = orderService.createOrder(
                            userEmail,
                            request.customerName(),
                            request.phoneNumber(),
                            request.shippingAddress(),
                            cartService.getUserCart(userEmail),
                            request.paymentMethod(),
                            request.paymentDetail(),
                            request.paymentPlan(),
                            request.installmentMonths());
                    cartService.clearCart(userEmail, null);
                    return order;
                });
        return apiMapper.toOrderResponse(created);
    }

    private record PlaceOrderRequest(
            @NotBlank(message = "Customer name is required.")
            @Size(max = 120, message = "Customer name is too long.")
            String customerName,
            @NotBlank(message = "Phone number is required.")
            @Size(max = 30, message = "Phone number is too long.")
            @Pattern(regexp = "^[0-9+()\\-\\s]{6,30}$", message = "Phone number format is invalid.")
            String phoneNumber,
            @NotBlank(message = "Shipping address is required.")
            @Size(max = 255, message = "Shipping address is too long.")
            String shippingAddress,
            @NotBlank(message = "Payment method is required.")
            String paymentMethod,
            String paymentDetail,
            @NotBlank(message = "Payment plan is required.")
            String paymentPlan,
            Integer installmentMonths) {
    }

    public record UserOrderPageResponse(
            List<OrderResponse> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize) {
    }
}

