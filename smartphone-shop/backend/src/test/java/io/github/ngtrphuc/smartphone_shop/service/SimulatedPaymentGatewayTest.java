package io.github.ngtrphuc.smartphone_shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import io.github.ngtrphuc.smartphone_shop.config.PaymentSimulationProperties;
import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;

class SimulatedPaymentGatewayTest {

    @Test
    void authorize_shouldReturnStableApprovalForCashOnDelivery() {
        PaymentSimulationProperties properties = new PaymentSimulationProperties();
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(properties);

        OrderCreatedEvent event = event(101L, "ORDER-101", "CASH_ON_DELIVERY");
        SimulatedPaymentGateway.PaymentAuthorizationResult first = gateway.authorize(event);
        SimulatedPaymentGateway.PaymentAuthorizationResult replay = gateway.authorize(event);

        assertEquals(SimulatedPaymentGateway.PaymentStatus.APPROVED, first.status());
        assertTrue(first.terminal());
        assertEquals(first.gatewayReference(), replay.gatewayReference());
        assertEquals(first.status(), replay.status());
    }

    @Test
    void authorize_shouldDecline_whenDeclineRateIs100Percent() {
        PaymentSimulationProperties properties = new PaymentSimulationProperties();
        properties.setDeclinePercent(100);
        properties.setRetryableFailurePercent(0);
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(properties);

        OrderCreatedEvent event = event(102L, "ORDER-102", "MASTERCARD");
        SimulatedPaymentGateway.PaymentAuthorizationResult first = gateway.authorize(event);
        SimulatedPaymentGateway.PaymentAuthorizationResult replay = gateway.authorize(event);

        assertEquals(SimulatedPaymentGateway.PaymentStatus.DECLINED, first.status());
        assertTrue(first.terminal());
        assertEquals(first.status(), replay.status());
        assertEquals(first.gatewayReference(), replay.gatewayReference());
    }

    @Test
    void authorize_shouldRetryThenApprove_whenRetryableRateIs100Percent() {
        PaymentSimulationProperties properties = new PaymentSimulationProperties();
        properties.setDeclinePercent(0);
        properties.setRetryableFailurePercent(100);
        properties.setMaxAuthorizeAttempts(3);
        SimulatedPaymentGateway gateway = new SimulatedPaymentGateway(properties);

        OrderCreatedEvent event = event(103L, "ORDER-103", "MASTERCARD");
        SimulatedPaymentGateway.PaymentAuthorizationResult first = gateway.authorize(event);
        SimulatedPaymentGateway.PaymentAuthorizationResult second = gateway.authorize(event);
        SimulatedPaymentGateway.PaymentAuthorizationResult third = gateway.authorize(event);
        SimulatedPaymentGateway.PaymentAuthorizationResult replay = gateway.authorize(event);

        assertEquals(SimulatedPaymentGateway.PaymentStatus.RETRYABLE_FAILURE, first.status());
        assertEquals(SimulatedPaymentGateway.PaymentStatus.RETRYABLE_FAILURE, second.status());
        assertEquals(SimulatedPaymentGateway.PaymentStatus.APPROVED, third.status());
        assertTrue(third.terminal());
        assertEquals(SimulatedPaymentGateway.PaymentStatus.APPROVED, replay.status());
        assertEquals(third.gatewayReference(), replay.gatewayReference());
    }

    private OrderCreatedEvent event(Long orderId, String orderCode, String paymentMethod) {
        return new OrderCreatedEvent(
                orderId,
                "user@example.com",
                orderCode,
                2999.0,
                1,
                paymentMethod,
                "FULL_PAYMENT",
                LocalDateTime.of(2026, 4, 23, 12, 0));
    }
}
