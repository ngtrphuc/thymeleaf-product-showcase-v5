package io.github.ngtrphuc.smartphone_shop.service;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import io.github.ngtrphuc.smartphone_shop.config.PaymentSimulationProperties;
import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;

@Service
@EnableConfigurationProperties(PaymentSimulationProperties.class)
public class SimulatedPaymentGateway {

    private final PaymentSimulationProperties properties;
    private final ConcurrentMap<Long, PaymentAuthorizationResult> terminalResults = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, Integer> attemptCounters = new ConcurrentHashMap<>();

    public SimulatedPaymentGateway(PaymentSimulationProperties properties) {
        this.properties = properties;
    }

    public PaymentAuthorizationResult authorize(OrderCreatedEvent event) {
        if (event == null || event.orderId() == null) {
            throw new IllegalArgumentException("OrderCreatedEvent with orderId is required for payment authorization.");
        }

        long orderId = event.orderId();
        PaymentAuthorizationResult replayed = terminalResults.get(orderId);
        if (replayed != null) {
            return replayed;
        }

        if (!properties.isEnabled()) {
            PaymentAuthorizationResult approved = PaymentAuthorizationResult.approved(
                    gatewayReference(event, deterministicScore(event)),
                    "payment simulation disabled");
            terminalResults.put(orderId, approved);
            return approved;
        }

        if (isCashOnDelivery(event.paymentMethod())) {
            PaymentAuthorizationResult approved = PaymentAuthorizationResult.approved(
                    "COD-" + safeOrderCode(event),
                    "cash on delivery does not require online authorization");
            terminalResults.put(orderId, approved);
            return approved;
        }

        int score = deterministicScore(event);
        Integer attemptValue = attemptCounters.compute(orderId,
                (ignoredOrderId, currentAttempt) -> currentAttempt == null ? 1 : currentAttempt + 1);
        int attempt = attemptValue == null ? 1 : attemptValue;

        int declineThreshold = properties.getDeclinePercent();
        int retryableThreshold = Math.min(100, declineThreshold + properties.getRetryableFailurePercent());

        if (score < declineThreshold) {
            PaymentAuthorizationResult declined = PaymentAuthorizationResult.declined(
                    gatewayReference(event, score),
                    "simulated risk rule declined this payment");
            terminalResults.put(orderId, declined);
            attemptCounters.remove(orderId);
            return declined;
        }

        int maxAttempts = properties.getMaxAuthorizeAttempts();
        if (score < retryableThreshold && attempt < maxAttempts) {
            return PaymentAuthorizationResult.retryableFailure(
                    gatewayReference(event, score),
                    "temporary gateway failure (simulated), please retry");
        }

        PaymentAuthorizationResult approved = PaymentAuthorizationResult.approved(
                gatewayReference(event, score),
                "payment authorized");
        terminalResults.put(orderId, approved);
        attemptCounters.remove(orderId);
        return approved;
    }

    public int maxAuthorizeAttempts() {
        return properties.getMaxAuthorizeAttempts();
    }

    private boolean isCashOnDelivery(String paymentMethod) {
        return "CASH_ON_DELIVERY".equalsIgnoreCase(Objects.requireNonNullElse(paymentMethod, ""));
    }

    private String safeOrderCode(OrderCreatedEvent event) {
        String orderCode = Objects.requireNonNullElse(event.orderCode(), "").trim();
        if (orderCode.isBlank()) {
            return "ORDER-" + event.orderId();
        }
        return orderCode.toUpperCase(Locale.ROOT);
    }

    private String gatewayReference(OrderCreatedEvent event, int score) {
        return "SIM-" + safeOrderCode(event) + "-" + String.format(Locale.ROOT, "%02d", score);
    }

    private int deterministicScore(OrderCreatedEvent event) {
        String payload = String.join("|",
                safeOrderCode(event),
                Objects.requireNonNullElse(event.userEmail(), "").toLowerCase(Locale.ROOT),
                Objects.requireNonNullElse(event.paymentMethod(), "").toUpperCase(Locale.ROOT),
                Objects.requireNonNullElse(event.paymentPlan(), "").toUpperCase(Locale.ROOT),
                event.totalAmount() == null ? "0.0" : String.format(Locale.ROOT, "%.2f", event.totalAmount()));
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (byte value : bytes) {
            hash = 31 * hash + value;
        }
        return Math.floorMod(hash, 100);
    }

    public enum PaymentStatus {
        APPROVED,
        RETRYABLE_FAILURE,
        DECLINED
    }

    public record PaymentAuthorizationResult(
            PaymentStatus status,
            boolean terminal,
            String gatewayReference,
            String reason) {
        public static PaymentAuthorizationResult approved(String gatewayReference, String reason) {
            return new PaymentAuthorizationResult(PaymentStatus.APPROVED, true, gatewayReference, reason);
        }

        public static PaymentAuthorizationResult retryableFailure(String gatewayReference, String reason) {
            return new PaymentAuthorizationResult(PaymentStatus.RETRYABLE_FAILURE, false, gatewayReference, reason);
        }

        public static PaymentAuthorizationResult declined(String gatewayReference, String reason) {
            return new PaymentAuthorizationResult(PaymentStatus.DECLINED, true, gatewayReference, reason);
        }
    }
}
