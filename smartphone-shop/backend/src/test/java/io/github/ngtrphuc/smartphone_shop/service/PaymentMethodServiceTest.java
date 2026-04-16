package io.github.ngtrphuc.smartphone_shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.repository.PaymentMethodRepository;

@ExtendWith(MockitoExtension.class)
class PaymentMethodServiceTest {

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @InjectMocks
    private PaymentMethodService paymentMethodService;

    @Test
    void addPaymentMethod_shouldSetFirstMethodAsDefault() {
        when(paymentMethodRepository.countActiveByUser("user@example.com")).thenReturn(0L);
        when(paymentMethodRepository.save(MockitoNullSafety.anyNonNull(PaymentMethod.class)))
                .thenAnswer(MockitoNullSafety.returnsFirstArgument(PaymentMethod.class));

        PaymentMethod created = paymentMethodService.addPaymentMethod(
                "User@Example.com",
                PaymentMethod.Type.CASH_ON_DELIVERY,
                null,
                false);

        assertEquals("user@example.com", created.getUserEmail());
        assertEquals(PaymentMethod.Type.CASH_ON_DELIVERY, created.getType());
        assertTrue(created.isDefault());
        verify(paymentMethodRepository).clearDefaultForUser("user@example.com");
    }

    @Test
    void addPaymentMethod_shouldRejectBankTransferWithoutDetail() {
        when(paymentMethodRepository.countActiveByUser("user@example.com")).thenReturn(1L);

        ValidationException exception = assertThrows(ValidationException.class, () -> paymentMethodService.addPaymentMethod(
                "user@example.com",
                PaymentMethod.Type.BANK_TRANSFER,
                "   ",
                false));
        assertNotNull(exception.getMessage());
    }

    @Test
    void addPaymentMethod_shouldAllowMasterCardWithoutDetail() {
        when(paymentMethodRepository.countActiveByUser("user@example.com")).thenReturn(1L);
        when(paymentMethodRepository.save(MockitoNullSafety.anyNonNull(PaymentMethod.class)))
                .thenAnswer(MockitoNullSafety.returnsFirstArgument(PaymentMethod.class));

        PaymentMethod created = paymentMethodService.addPaymentMethod(
                "user@example.com",
                PaymentMethod.Type.MASTERCARD,
                null,
                false);

        assertEquals(PaymentMethod.Type.MASTERCARD, created.getType());
        assertNull(created.getDetail());
    }

    @Test
    void remove_shouldPromoteNextMethodWhenDefaultIsRemoved() {
        PaymentMethod defaultMethod = new PaymentMethod();
        defaultMethod.setId(1L);
        defaultMethod.setUserEmail("user@example.com");
        defaultMethod.setType(PaymentMethod.Type.CASH_ON_DELIVERY);
        defaultMethod.setDefault(true);
        defaultMethod.setActive(true);

        PaymentMethod nextMethod = new PaymentMethod();
        nextMethod.setId(2L);
        nextMethod.setUserEmail("user@example.com");
        nextMethod.setType(PaymentMethod.Type.PAYPAY);
        nextMethod.setDefault(false);
        nextMethod.setActive(true);

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(defaultMethod));
        when(paymentMethodRepository.findByUserEmailAndActiveTrueOrderByIsDefaultDescCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(nextMethod));

        paymentMethodService.remove("user@example.com", 1L);

        assertFalse(defaultMethod.isDefault());
        assertFalse(defaultMethod.isActive());
        assertTrue(nextMethod.isDefault());
        verify(paymentMethodRepository).save(defaultMethod);
        verify(paymentMethodRepository).save(nextMethod);
    }
}
