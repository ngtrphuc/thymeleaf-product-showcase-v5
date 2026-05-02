package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.OperationStatusResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OrderResponse;
import io.github.ngtrphuc.smartphone_shop.service.OrderReturnService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminOrderApiController {

    private final OrderService orderService;
    private final OrderReturnService orderReturnService;
    private final ApiMapper apiMapper;

    public AdminOrderApiController(
            OrderService orderService,
            OrderReturnService orderReturnService,
            ApiMapper apiMapper) {
        this.orderService = orderService;
        this.orderReturnService = orderReturnService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/orders")
    public AdminOrderPageResponse orders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(pageSize, 50));

        long totalOrders = orderService.countOrders();
        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / safeSize);
        int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(safePage, totalPages - 1));

        List<OrderResponse> orders = orderService.getAdminOrdersPage(currentPage, safeSize)
                .stream()
                .map(apiMapper::toOrderResponse)
                .toList();
        return new AdminOrderPageResponse(orders, currentPage, totalPages, totalOrders, safeSize);
    }

    @PostMapping("/orders/{id}/status")
    public OperationStatusResponse updateOrderStatus(@PathVariable(name = "id") long id,
            @RequestBody UpdateOrderStatusRequest request) {
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new IllegalArgumentException("Order status is required.");
        }
        orderService.updateStatus(id, request.status());
        return new OperationStatusResponse(true, "Order status updated.");
    }

    @PostMapping("/orders/{id}/ship")
    public OperationStatusResponse shipOrder(
            @PathVariable(name = "id") long id,
            @Valid @RequestBody ShipOrderRequest request) {
        orderService.shipOrder(id, request.trackingNumber(), request.carrier());
        return new OperationStatusResponse(true, "Order shipped.");
    }

    @PostMapping("/orders/{id}/return/approve")
    public OperationStatusResponse approveReturn(
            @PathVariable(name = "id") long id,
            @Valid @RequestBody ApproveReturnRequest request) {
        orderReturnService.approveReturn(id, request.refundAmount(), request.adminNote());
        return new OperationStatusResponse(true, "Return approved.");
    }

    @PostMapping("/orders/{id}/return/reject")
    public OperationStatusResponse rejectReturn(
            @PathVariable(name = "id") long id,
            @Valid @RequestBody RejectReturnRequest request) {
        orderReturnService.rejectReturn(id, request.adminNote());
        return new OperationStatusResponse(true, "Return rejected.");
    }

    @PostMapping("/orders/{id}/return/refund")
    public OperationStatusResponse refundReturn(
            @PathVariable(name = "id") long id,
            @Valid @RequestBody ApproveReturnRequest request) {
        orderReturnService.markRefunded(id, request.refundAmount(), request.adminNote());
        return new OperationStatusResponse(true, "Return marked as refunded.");
    }

    public record AdminOrderPageResponse(
            List<OrderResponse> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize) {
    }

    public record UpdateOrderStatusRequest(String status) {
    }

    public record ShipOrderRequest(
            @NotBlank(message = "Tracking number is required.")
            @Size(max = 100, message = "Tracking number is too long.")
            String trackingNumber,
            @NotBlank(message = "Carrier is required.")
            @Size(max = 50, message = "Carrier is too long.")
            String carrier) {
    }

    public record ApproveReturnRequest(
            @PositiveOrZero(message = "Refund amount cannot be negative.")
            Double refundAmount,
            @Size(max = 500, message = "Admin note is too long.")
            String adminNote) {
    }

    public record RejectReturnRequest(
            @Size(max = 500, message = "Admin note is too long.")
            String adminNote) {
    }
}
