package io.github.ngtrphuc.smartphone_shop.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.common.exception.UnauthorizedActionException;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderReturn;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;
import io.github.ngtrphuc.smartphone_shop.repository.OrderReturnRepository;

@Service
public class OrderReturnService {

    private final OrderRepository orderRepository;
    private final OrderReturnRepository orderReturnRepository;
    private final OrderService orderService;

    public OrderReturnService(OrderRepository orderRepository,
            OrderReturnRepository orderReturnRepository,
            OrderService orderService) {
        this.orderRepository = orderRepository;
        this.orderReturnRepository = orderReturnRepository;
        this.orderService = orderService;
    }

    @Transactional
    public void requestReturn(Long orderId, String userEmail, String reason) {
        String normalizedReason = normalizeReason(reason);
        Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
                .orElseThrow(() -> new OrderValidationException("Order not found."));

        if (!order.getUserEmail().equalsIgnoreCase(userEmail)) {
            throw new UnauthorizedActionException("You do not have permission to request a return for this order.");
        }
        if (orderReturnRepository.existsByOrderId(orderId)) {
            throw new OrderValidationException("Return request already exists for this order.");
        }

        orderService.updateStatus(orderId, "return_requested");

        OrderReturn orderReturn = new OrderReturn();
        orderReturn.setOrder(order);
        orderReturn.setReason(normalizedReason);
        orderReturn.setStatus("REQUESTED");
        orderReturn.setRequestedAt(LocalDateTime.now());
        orderReturnRepository.save(orderReturn);
    }

    @Transactional
    public void approveReturn(Long orderId, Double refundAmount, String adminNote) {
        OrderReturn orderReturn = orderReturnRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderValidationException("Return request not found."));
        if (!"REQUESTED".equalsIgnoreCase(orderReturn.getStatus())) {
            throw new OrderValidationException("Only requested returns can be approved.");
        }

        double resolvedRefund = normalizeRefundAmount(refundAmount, orderReturn.getOrder());
        orderReturn.setStatus("APPROVED");
        orderReturn.setRefundAmount(resolvedRefund);
        orderReturn.setAdminNote(normalizeAdminNote(adminNote));
        orderReturn.setResolvedAt(LocalDateTime.now());
        orderReturnRepository.save(orderReturn);

        orderService.updateStatus(orderId, "return_approved");
    }

    @Transactional
    public void rejectReturn(Long orderId, String adminNote) {
        OrderReturn orderReturn = orderReturnRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderValidationException("Return request not found."));
        if (!"REQUESTED".equalsIgnoreCase(orderReturn.getStatus())) {
            throw new OrderValidationException("Only requested returns can be rejected.");
        }

        orderReturn.setStatus("REJECTED");
        orderReturn.setAdminNote(normalizeAdminNote(adminNote));
        orderReturn.setResolvedAt(LocalDateTime.now());
        orderReturnRepository.save(orderReturn);

        orderService.updateStatus(orderId, "return_rejected");
    }

    @Transactional
    public void markRefunded(Long orderId, Double refundAmount, String adminNote) {
        OrderReturn orderReturn = orderReturnRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderValidationException("Return request not found."));
        if (!"APPROVED".equalsIgnoreCase(orderReturn.getStatus())) {
            throw new OrderValidationException("Only approved returns can be marked as refunded.");
        }

        double resolvedRefund = normalizeRefundAmount(refundAmount, orderReturn.getOrder());
        orderReturn.setStatus("REFUNDED");
        orderReturn.setRefundAmount(resolvedRefund);
        orderReturn.setAdminNote(normalizeAdminNote(adminNote));
        orderReturn.setResolvedAt(LocalDateTime.now());
        orderReturnRepository.save(orderReturn);

        orderService.updateStatus(orderId, "refunded");
    }

    private String normalizeReason(String reason) {
        String normalized = reason == null ? "" : reason.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new OrderValidationException("Return reason is required.");
        }
        if (normalized.length() > 500) {
            throw new OrderValidationException("Return reason is too long.");
        }
        return normalized;
    }

    private String normalizeAdminNote(String note) {
        if (note == null) {
            return null;
        }
        String normalized = note.trim().replaceAll("\\s+", " ");
        if (normalized.length() > 500) {
            throw new OrderValidationException("Admin note is too long.");
        }
        return normalized.isBlank() ? null : normalized;
    }

    private double normalizeRefundAmount(Double refundAmount, Order order) {
        double orderTotal = order.getTotalAmount() != null ? order.getTotalAmount() : 0.0;
        double resolved = refundAmount != null ? refundAmount : orderTotal;
        if (resolved < 0) {
            throw new OrderValidationException("Refund amount cannot be negative.");
        }
        if (resolved > orderTotal) {
            throw new OrderValidationException("Refund amount cannot exceed order total.");
        }
        return resolved;
    }
}
