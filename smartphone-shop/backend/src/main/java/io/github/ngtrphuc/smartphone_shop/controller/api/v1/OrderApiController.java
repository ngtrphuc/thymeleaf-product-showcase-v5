package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderApiController {

    private final OrderService orderService;
    private final ApiMapper apiMapper;

    public OrderApiController(OrderService orderService, ApiMapper apiMapper) {
        this.orderService = orderService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public List<OrderResponse> orders(Authentication authentication) {
        return orderService.getOrdersByUser(authentication.getName()).stream()
                .map(apiMapper::toOrderResponse)
                .toList();
    }

    @PostMapping("/{id}/cancel")
    public OperationStatusResponse cancel(@PathVariable Long id, Authentication authentication) {
        boolean success = orderService.cancelOrder(id, authentication.getName());
        return new OperationStatusResponse(
                success,
                success ? "Order cancelled successfully." : "Cannot cancel this order.");
    }
}

