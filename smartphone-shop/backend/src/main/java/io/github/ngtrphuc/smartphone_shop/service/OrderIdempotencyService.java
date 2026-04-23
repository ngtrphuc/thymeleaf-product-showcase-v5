package io.github.ngtrphuc.smartphone_shop.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Supplier;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderIdempotencyKey;
import io.github.ngtrphuc.smartphone_shop.repository.OrderIdempotencyKeyRepository;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;

@Service
public class OrderIdempotencyService {

    private static final int MAX_KEY_LENGTH = 120;
    private static final Duration STALE_PLACEHOLDER_TIMEOUT = Duration.ofMinutes(5);

    private final OrderIdempotencyKeyRepository orderIdempotencyKeyRepository;
    private final OrderRepository orderRepository;

    public OrderIdempotencyService(OrderIdempotencyKeyRepository orderIdempotencyKeyRepository,
            OrderRepository orderRepository) {
        this.orderIdempotencyKeyRepository = orderIdempotencyKeyRepository;
        this.orderRepository = orderRepository;
    }

    @Transactional
    public Order executeCheckout(String userEmail,
            String idempotencyKey,
            String requestFingerprint,
            Supplier<Order> orderCreator) {
        String normalizedEmail = normalizeEmail(userEmail);
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String normalizedFingerprint = normalizeFingerprint(requestFingerprint);

        OrderIdempotencyKey existing = orderIdempotencyKeyRepository
                .findByUserEmailAndIdempotencyKey(normalizedEmail, normalizedKey)
                .orElse(null);
        if (existing != null) {
            return resolveExistingOrder(existing, normalizedFingerprint);
        }

        OrderIdempotencyKey placeholder = new OrderIdempotencyKey();
        placeholder.setUserEmail(normalizedEmail);
        placeholder.setIdempotencyKey(normalizedKey);
        placeholder.setRequestFingerprint(normalizedFingerprint);

        try {
            orderIdempotencyKeyRepository.saveAndFlush(placeholder);
        } catch (DataIntegrityViolationException ex) {
            OrderIdempotencyKey concurrent = orderIdempotencyKeyRepository
                    .findByUserEmailAndIdempotencyKey(normalizedEmail, normalizedKey)
                    .orElseThrow(() -> new IllegalStateException(
                            "Failed to resolve idempotent checkout request after key collision.", ex));
            return resolveExistingOrder(concurrent, normalizedFingerprint);
        }

        Order created;
        try {
            created = Objects.requireNonNull(orderCreator.get(), "Order creator must not return null.");
        } catch (RuntimeException ex) {
            orderIdempotencyKeyRepository.delete(placeholder);
            throw ex;
        }
        placeholder.setOrderId(created.getId());
        orderIdempotencyKeyRepository.save(placeholder);
        return created;
    }

    public String createFingerprint(String userEmail,
            String customerName,
            String phoneNumber,
            String shippingAddress,
            String paymentMethod,
            String paymentDetail,
            String paymentPlan,
            Integer installmentMonths) {
        String payload = String.join("\u0000",
                normalizeEmail(userEmail),
                normalizeText(customerName),
                normalizeText(phoneNumber),
                normalizeText(shippingAddress),
                normalizeText(paymentMethod).toUpperCase(Locale.ROOT),
                normalizeText(paymentDetail),
                normalizeText(paymentPlan).toUpperCase(Locale.ROOT),
                installmentMonths == null ? "-" : installmentMonths.toString());
        return sha256Hex(payload);
    }

    private Order resolveExistingOrder(OrderIdempotencyKey existing, String requestFingerprint) {
        if (!Objects.equals(existing.getRequestFingerprint(), requestFingerprint)) {
            throw new IllegalStateException(
                    "This idempotency key was already used for a different checkout payload.");
        }
        if (existing.getOrderId() == null) {
            if (isStalePlaceholder(existing)) {
                orderIdempotencyKeyRepository.delete(existing);
                throw new OrderValidationException(
                        "The previous checkout attempt timed out. Please retry this request.");
            }
            throw new IllegalStateException(
                "A checkout with this idempotency key is already being processed. Please retry shortly.");
        }
        return orderRepository.findByIdWithItems(existing.getOrderId())
                .orElseThrow(() -> new IllegalStateException("Existing idempotent order could not be loaded."));
    }

    @Scheduled(fixedDelayString = "${app.order.idempotency.cleanup-delay-ms:300000}")
    @Transactional
    public void cleanupStalePlaceholders() {
        LocalDateTime cutoff = LocalDateTime.now().minus(STALE_PLACEHOLDER_TIMEOUT);
        orderIdempotencyKeyRepository.deleteStalePlaceholders(cutoff);
    }

    private boolean isStalePlaceholder(OrderIdempotencyKey key) {
        LocalDateTime createdAt = key.getCreatedAt();
        if (createdAt == null) {
            return true;
        }
        return createdAt.isBefore(LocalDateTime.now().minus(STALE_PLACEHOLDER_TIMEOUT));
    }

    private String normalizeIdempotencyKey(String idempotencyKey) {
        String normalized = normalizeText(idempotencyKey);
        if (normalized.isBlank()) {
            throw new OrderValidationException("Idempotency-Key header is required.");
        }
        if (normalized.length() > MAX_KEY_LENGTH) {
            throw new OrderValidationException("Idempotency-Key header is too long.");
        }
        return normalized;
    }

    private String normalizeFingerprint(String requestFingerprint) {
        String normalized = normalizeText(requestFingerprint);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("Idempotency fingerprint must not be blank.");
        }
        return normalized;
    }

    private String normalizeEmail(String email) {
        String normalized = normalizeText(email).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("User email is required for idempotent checkout.");
        }
        return normalized;
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.trim().replaceAll("\\s+", " ");
    }

    private String sha256Hex(String payload) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte value : hash) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 hashing is required for idempotency fingerprints.", ex);
        }
    }
}
