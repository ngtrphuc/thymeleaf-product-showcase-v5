package io.github.ngtrphuc.smartphone_shop.service;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;

@ExtendWith(MockitoExtension.class)
class OrderWorkflowProcessorTest {

    @Mock
    private OrderService orderService;

    @Mock
    private SimulatedPaymentGateway paymentGateway;

    @Mock
    private EmailSender emailSender;

    private OrderWorkflowProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new OrderWorkflowProcessor(orderService, paymentGateway, emailSender);
    }

    @Test
    void handleOrderCreated_shouldMoveOrderToProcessing_whenPaymentApproved() {
        OrderCreatedEvent event = event(120L, "ORD-120");
        when(paymentGateway.maxAuthorizeAttempts()).thenReturn(3);
        when(paymentGateway.authorize(event)).thenReturn(
                SimulatedPaymentGateway.PaymentAuthorizationResult.approved("SIM-ORD-120-00", "ok"));

        processor.handleOrderCreated(event);

        verify(paymentGateway, times(1)).authorize(event);
        verify(orderService).updateStatus(120L, "processing");
    }

    @Test
    void handleOrderCreated_shouldCancelOrder_whenPaymentDeclined() {
        OrderCreatedEvent event = event(121L, "ORD-121");
        when(paymentGateway.maxAuthorizeAttempts()).thenReturn(3);
        when(paymentGateway.authorize(event)).thenReturn(
                SimulatedPaymentGateway.PaymentAuthorizationResult.declined("SIM-ORD-121-01", "declined"));

        processor.handleOrderCreated(event);

        verify(paymentGateway, times(1)).authorize(event);
        verify(orderService).updateStatus(121L, "cancelled");
    }

    @Test
    void handleOrderCreated_shouldCancelAfterExhaustedRetries_whenGatewayStaysRetryable() {
        OrderCreatedEvent event = event(122L, "ORD-122");
        when(paymentGateway.maxAuthorizeAttempts()).thenReturn(2);
        when(paymentGateway.authorize(event)).thenReturn(
                SimulatedPaymentGateway.PaymentAuthorizationResult.retryableFailure("SIM-ORD-122-20", "temp"),
                SimulatedPaymentGateway.PaymentAuthorizationResult.retryableFailure("SIM-ORD-122-20", "temp"));

        processor.handleOrderCreated(event);

        verify(paymentGateway, times(2)).authorize(event);
        verify(orderService).updateStatus(122L, "cancelled");
    }

    private OrderCreatedEvent event(Long orderId, String code) {
        return new OrderCreatedEvent(
                orderId,
                "user@example.com",
                code,
                5000.0,
                2,
                "MASTERCARD",
                "FULL_PAYMENT",
                LocalDateTime.of(2026, 4, 23, 14, 30));
    }
}
