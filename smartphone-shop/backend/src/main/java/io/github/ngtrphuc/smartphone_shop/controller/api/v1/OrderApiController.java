package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
    public List<OrderResponse> orders(Authentication authentication) {
        return orderService.getOrdersByUser(authentication.getName()).stream()
                .map(apiMapper::toOrderResponse)
                .toList();
    }

    @PostMapping("/{id}/cancel")
    public OperationStatusResponse cancel(@PathVariable(name = "id") Long id, Authentication authentication) {
        boolean success = orderService.cancelOrder(id, authentication.getName());
        return new OperationStatusResponse(
                success,
                success ? "Order cancelled successfully." : "Cannot cancel this order.");
    }

    @PostMapping
    @Transactional
    public OrderResponse place(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody PlaceOrderRequest request,
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
            String customerName,
            String phoneNumber,
            String shippingAddress,
            String paymentMethod,
            String paymentDetail,
            String paymentPlan,
            Integer installmentMonths) {
    }
}

