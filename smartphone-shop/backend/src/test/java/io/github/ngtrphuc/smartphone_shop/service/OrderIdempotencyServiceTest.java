package io.github.ngtrphuc.smartphone_shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderIdempotencyKey;
import io.github.ngtrphuc.smartphone_shop.repository.OrderIdempotencyKeyRepository;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderIdempotencyServiceTest {

    @Mock
    private OrderIdempotencyKeyRepository orderIdempotencyKeyRepository;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderIdempotencyService orderIdempotencyService;

    @Test
    void executeCheckout_shouldReturnExistingOrder_whenSameKeyAndFingerprintReplay() {
        Order existingOrder = buildOrder(10L);
        OrderIdempotencyKey key = new OrderIdempotencyKey();
        key.setId(1L);
        key.setUserEmail("user@example.com");
        key.setIdempotencyKey("checkout-key");
        key.setRequestFingerprint("fingerprint-1");
        key.setOrderId(10L);

        when(orderIdempotencyKeyRepository.findByUserEmailAndIdempotencyKey("user@example.com", "checkout-key"))
                .thenReturn(Optional.of(key));
        when(orderRepository.findByIdWithItems(10L)).thenReturn(Optional.of(existingOrder));

        Order replayed = orderIdempotencyService.executeCheckout(
                "user@example.com",
                "checkout-key",
                "fingerprint-1",
                () -> buildOrder(11L));

        assertSame(existingOrder, replayed);
        verify(orderIdempotencyKeyRepository)
                .findByUserEmailAndIdempotencyKey("user@example.com", "checkout-key");
        verifyNoMoreInteractions(orderIdempotencyKeyRepository);
    }

    @Test
    void executeCheckout_shouldRejectReusedKey_whenFingerprintDiffers() {
        OrderIdempotencyKey key = new OrderIdempotencyKey();
        key.setUserEmail("user@example.com");
        key.setIdempotencyKey("checkout-key");
        key.setRequestFingerprint("fingerprint-1");
        key.setOrderId(10L);

        when(orderIdempotencyKeyRepository.findByUserEmailAndIdempotencyKey("user@example.com", "checkout-key"))
                .thenReturn(Optional.of(key));

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> orderIdempotencyService.executeCheckout(
                "user@example.com",
                "checkout-key",
                "fingerprint-2",
                () -> buildOrder(11L)));

        assertEquals("This idempotency key was already used for a different checkout payload.", ex.getMessage());
        verify(orderRepository, never()).findByIdWithItems(anyLong());
    }

    @Test
    void executeCheckout_shouldDeleteStalePlaceholderAndRequestRetry() {
        OrderIdempotencyKey key = new OrderIdempotencyKey();
        key.setUserEmail("user@example.com");
        key.setIdempotencyKey("checkout-key");
        key.setRequestFingerprint("fingerprint-1");
        key.setOrderId(null);
        key.setCreatedAt(LocalDateTime.now().minusMinutes(10));

        when(orderIdempotencyKeyRepository.findByUserEmailAndIdempotencyKey("user@example.com", "checkout-key"))
                .thenReturn(Optional.of(key));

        OrderValidationException ex = assertThrows(OrderValidationException.class, () -> orderIdempotencyService.executeCheckout(
                "user@example.com",
                "checkout-key",
                "fingerprint-1",
                () -> {
                    throw new AssertionError("Order creator must not run when stale placeholder is detected.");
                }));

        assertEquals("The previous checkout attempt timed out. Please retry this request.", ex.getMessage());
        verify(orderIdempotencyKeyRepository).delete(key);
        verify(orderRepository, never()).findByIdWithItems(anyLong());
    }

    @Test
    void executeCheckout_shouldDeletePlaceholderWhenOrderCreationFails() {
        when(orderIdempotencyKeyRepository.findByUserEmailAndIdempotencyKey("user@example.com", "checkout-key"))
                .thenReturn(Optional.empty());

        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> orderIdempotencyService.executeCheckout(
                "user@example.com",
                "checkout-key",
                "fingerprint-1",
                () -> {
                    throw new IllegalStateException("checkout failure");
                }));

        assertEquals("checkout failure", ex.getMessage());
        verify(orderIdempotencyKeyRepository).saveAndFlush(MockitoNullSafety.anyNonNull(OrderIdempotencyKey.class));
        verify(orderIdempotencyKeyRepository).delete(MockitoNullSafety.anyNonNull(OrderIdempotencyKey.class));
    }

    @Test
    void cleanupStalePlaceholders_shouldDeletePendingRowsBeforeCutoff() {
        orderIdempotencyService.cleanupStalePlaceholders();

        verify(orderIdempotencyKeyRepository).deleteStalePlaceholders(MockitoNullSafety.anyNonNull(LocalDateTime.class));
    }

    @Test
    void createFingerprint_shouldIgnoreCaseAndWhitespaceNoise() {
        String left = orderIdempotencyService.createFingerprint(
                " User@example.com ",
                " Nguyen   Phuc ",
                "0901234567",
                " Tokyo  Tower ",
                "cash_on_delivery",
                null,
                "full_payment",
                null);
        String right = orderIdempotencyService.createFingerprint(
                "user@example.com",
                "Nguyen Phuc",
                "0901234567",
                "Tokyo Tower",
                "CASH_ON_DELIVERY",
                "",
                "FULL_PAYMENT",
                null);

        assertEquals(left, right);
        assertNotEquals(left, orderIdempotencyService.createFingerprint(
                "user@example.com",
                "Nguyen Phuc",
                "0901234567",
                "Osaka",
                "CASH_ON_DELIVERY",
                "",
                "FULL_PAYMENT",
                null));
    }

    private Order buildOrder(long id) {
        Order order = new Order();
        order.setId(id);
        order.setUserEmail("user@example.com");
        order.setCustomerName("Nguyen Phuc");
        order.setPhoneNumber("0901234567");
        order.setShippingAddress("Tokyo");
        order.setStatus("pending");
        order.setCreatedAt(LocalDateTime.of(2026, 4, 22, 10, 0));
        return order;
    }
}
