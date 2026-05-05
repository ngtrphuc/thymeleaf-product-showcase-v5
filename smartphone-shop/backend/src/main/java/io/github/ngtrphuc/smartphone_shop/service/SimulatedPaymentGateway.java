package io.github.ngtrphuc.smartphone_shop.service;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.github.ngtrphuc.smartphone_shop.config.PaymentSimulationProperties;
import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;

@Service
@EnableConfigurationProperties(PaymentSimulationProperties.class)
@SuppressWarnings("null")
public class SimulatedPaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(SimulatedPaymentGateway.class);
    @NonNull
    private static final Duration PAYMENT_RESULT_TTL = Duration.ofDays(7);
    @NonNull
    private static final Duration ATTEMPT_COUNTER_TTL = Duration.ofHours(6);
    @NonNull
    private static final String TERMINAL_RESULT_PREFIX = "payment:terminal:";
    @NonNull
    private static final String ATTEMPT_COUNTER_PREFIX = "payment:attempt:";

    @NonNull
    private final PaymentSimulationProperties properties;
    @Nullable
    private final StringRedisTemplate redisTemplate;
    @NonNull
    private final ObjectMapper objectMapper;
    @NonNull
    private final ConcurrentMap<Long, PaymentAuthorizationResult> terminalResults = new ConcurrentHashMap<>();
    @NonNull
    private final ConcurrentMap<Long, Integer> attemptCounters = new ConcurrentHashMap<>();

    @Autowired
    public SimulatedPaymentGateway(PaymentSimulationProperties properties,
            @Nullable StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper) {
        this.properties = properties;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public SimulatedPaymentGateway(PaymentSimulationProperties properties) {
        this(properties, null, new ObjectMapper());
    }

    @NonNull
    public PaymentAuthorizationResult authorize(@Nullable OrderCreatedEvent event) {
        if (event == null || event.orderId() == null) {
            throw new IllegalArgumentException("OrderCreatedEvent with orderId is required for payment authorization.");
        }

        long orderId = event.orderId();
        PaymentAuthorizationResult replayed = getTerminalResult(orderId);
        if (replayed != null) {
            return replayed;
        }

        if (!properties.isEnabled()) {
            PaymentAuthorizationResult approved = PaymentAuthorizationResult.approved(
                    gatewayReference(event, deterministicScore(event)),
                    "payment simulation disabled");
            saveTerminalResult(orderId, approved);
            return approved;
        }

        if (isCashOnDelivery(event.paymentMethod())) {
            PaymentAuthorizationResult approved = PaymentAuthorizationResult.approved(
                    "COD-" + safeOrderCode(event),
                    "cash on delivery does not require online authorization");
            saveTerminalResult(orderId, approved);
            return approved;
        }

        int score = deterministicScore(event);
        int attempt = incrementAttemptCounter(orderId);

        int declineThreshold = properties.getDeclinePercent();
        int retryableThreshold = Math.min(100, declineThreshold + properties.getRetryableFailurePercent());

        if (score < declineThreshold) {
            PaymentAuthorizationResult declined = PaymentAuthorizationResult.declined(
                    gatewayReference(event, score),
                    "simulated risk rule declined this payment");
            saveTerminalResult(orderId, declined);
            clearAttemptCounter(orderId);
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
        saveTerminalResult(orderId, approved);
        clearAttemptCounter(orderId);
        return approved;
    }

    public int maxAuthorizeAttempts() {
        return properties.getMaxAuthorizeAttempts();
    }

    private boolean isCashOnDelivery(@Nullable String paymentMethod) {
        String resolvedPaymentMethod = paymentMethod == null ? "" : paymentMethod;
        return "CASH_ON_DELIVERY".equalsIgnoreCase(resolvedPaymentMethod);
    }

    @NonNull
    private String safeOrderCode(OrderCreatedEvent event) {
        String orderCode = nullToBlank(event.orderCode()).trim();
        if (orderCode.isBlank()) {
            return "ORDER-" + event.orderId();
        }
        return orderCode.toUpperCase(Locale.ROOT);
    }

    @NonNull
    private String gatewayReference(OrderCreatedEvent event, int score) {
        return "SIM-" + safeOrderCode(event) + "-" + String.format(Locale.ROOT, "%02d", score);
    }

    private int deterministicScore(OrderCreatedEvent event) {
        String payload = String.join("|",
                safeOrderCode(event),
                nullToBlank(event.userEmail()).toLowerCase(Locale.ROOT),
                nullToBlank(event.paymentMethod()).toUpperCase(Locale.ROOT),
                nullToBlank(event.paymentPlan()).toUpperCase(Locale.ROOT),
                event.totalAmount() == null ? "0.0" : String.format(Locale.ROOT, "%.2f", event.totalAmount()));
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        int hash = 0;
        for (byte value : bytes) {
            hash = 31 * hash + value;
        }
        return Math.floorMod(hash, 100);
    }

    private PaymentAuthorizationResult getTerminalResult(long orderId) {
        PaymentAuthorizationResult fallback = terminalResults.get(orderId);
        StringRedisTemplate redis = redisTemplate;
        if (fallback != null || redis == null) {
            return fallback;
        }
        try {
            String key = terminalResultKey(orderId);
            String payload = redis.opsForValue().get(key);
            if (payload == null || payload.isBlank()) {
                return null;
            }
            PaymentAuthorizationResult result = objectMapper.readValue(payload, PaymentAuthorizationResult.class);
            terminalResults.putIfAbsent(orderId, result);
            return result;
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to read simulated payment terminal result from Redis for orderId={}.", orderId, ex);
            return fallback;
        }
    }

    private void saveTerminalResult(long orderId, PaymentAuthorizationResult result) {
        terminalResults.put(orderId, result);
        StringRedisTemplate redis = redisTemplate;
        if (redis == null) {
            return;
        }
        try {
            String key = terminalResultKey(orderId);
            String payload = Objects.requireNonNull(objectMapper.writeValueAsString(result));
            redis.opsForValue().set(key, payload, PAYMENT_RESULT_TTL);
        } catch (RuntimeException | JsonProcessingException ex) {
            log.warn("Failed to persist simulated payment terminal result to Redis for orderId={}.", orderId, ex);
        }
    }

    private int incrementAttemptCounter(long orderId) {
        StringRedisTemplate redis = redisTemplate;
        if (redis != null) {
            try {
                String key = attemptCounterKey(orderId);
                Long value = redis.opsForValue().increment(key);
                redis.expire(key, ATTEMPT_COUNTER_TTL);
                return value == null ? 1 : Math.toIntExact(value);
            } catch (RuntimeException ex) {
                log.warn("Failed to increment simulated payment attempt counter in Redis for orderId={}.", orderId, ex);
            }
        }
        Integer attemptValue = attemptCounters.compute(orderId,
                (ignoredOrderId, currentAttempt) -> currentAttempt == null ? 1 : currentAttempt + 1);
        return attemptValue == null ? 1 : attemptValue;
    }

    private void clearAttemptCounter(long orderId) {
        attemptCounters.remove(orderId);
        StringRedisTemplate redis = redisTemplate;
        if (redis == null) {
            return;
        }
        try {
            redis.delete(attemptCounterKey(orderId));
        } catch (RuntimeException ex) {
            log.warn("Failed to clear simulated payment attempt counter in Redis for orderId={}.", orderId, ex);
        }
    }

    @NonNull
    private String terminalResultKey(long orderId) {
        return TERMINAL_RESULT_PREFIX + orderId;
    }

    @NonNull
    private String attemptCounterKey(long orderId) {
        return ATTEMPT_COUNTER_PREFIX + orderId;
    }

    @NonNull
    private String nullToBlank(@Nullable String value) {
        return value == null ? "" : value;
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
