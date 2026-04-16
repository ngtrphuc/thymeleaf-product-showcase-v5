package io.github.ngtrphuc.smartphone_shop.service;

import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.ResourceNotFoundException;
import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.repository.PaymentMethodRepository;

@Service
public class PaymentMethodService {

    private static final int MAX_PAYMENT_METHODS_PER_USER = 5;
    private static final int MAX_DETAIL_LENGTH = 200;
    private static final Set<PaymentMethod.Type> UNSUPPORTED_TYPES = Set.of(
            PaymentMethod.Type.KOMBINI,
            PaymentMethod.Type.VISA);

    private final PaymentMethodRepository paymentMethodRepository;

    public PaymentMethodService(PaymentMethodRepository paymentMethodRepository) {
        this.paymentMethodRepository = paymentMethodRepository;
    }

    @Transactional(readOnly = true)
    public List<PaymentMethod> getUserPaymentMethods(String email) {
        return paymentMethodRepository.findByUserEmailAndActiveTrueOrderByIsDefaultDescCreatedAtDesc(normalize(email))
                .stream()
                .filter(paymentMethod -> !UNSUPPORTED_TYPES.contains(paymentMethod.getType()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentMethod getDefaultPaymentMethod(String email) {
        return paymentMethodRepository
                .findByUserEmailAndIsDefaultTrueAndActiveTrue(normalize(email))
                .orElse(null);
    }

    @Transactional
    public PaymentMethod addPaymentMethod(String email, PaymentMethod.Type type, String detail, boolean setAsDefault) {
        String normalizedEmail = normalize(email);
        if (type == null) {
            throw new ValidationException("Payment method type is required.");
        }
        if (UNSUPPORTED_TYPES.contains(type)) {
            throw new ValidationException("This payment method is no longer supported.");
        }

        long count = paymentMethodRepository.countActiveByUser(normalizedEmail);
        if (count >= MAX_PAYMENT_METHODS_PER_USER) {
            throw new IllegalStateException("Maximum " + MAX_PAYMENT_METHODS_PER_USER + " payment methods allowed.");
        }

        String normalizedDetail = normalizeDetail(type, detail);

        if (setAsDefault || count == 0) {
            paymentMethodRepository.clearDefaultForUser(normalizedEmail);
        }

        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setUserEmail(normalizedEmail);
        paymentMethod.setType(type);
        paymentMethod.setDetail(normalizedDetail);
        paymentMethod.setDefault(setAsDefault || count == 0);
        paymentMethod.setActive(true);
        return paymentMethodRepository.save(paymentMethod);
    }

    @Transactional
    public void setDefault(String email, Long paymentMethodId) {
        if (paymentMethodId == null) {
            throw new ResourceNotFoundException("Payment method not found.");
        }
        String normalizedEmail = normalize(email);
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .filter(pm -> pm.isActive() && normalizedEmail.equals(pm.getUserEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found."));

        paymentMethodRepository.clearDefaultForUser(normalizedEmail);
        paymentMethod.setDefault(true);
        paymentMethodRepository.save(paymentMethod);
    }

    @Transactional
    public void remove(String email, Long paymentMethodId) {
        if (paymentMethodId == null) {
            throw new ResourceNotFoundException("Payment method not found.");
        }
        String normalizedEmail = normalize(email);
        PaymentMethod paymentMethod = paymentMethodRepository.findById(paymentMethodId)
                .filter(pm -> normalizedEmail.equals(pm.getUserEmail()))
                .orElseThrow(() -> new ResourceNotFoundException("Payment method not found."));

        paymentMethod.setActive(false);
        if (paymentMethod.isDefault()) {
            paymentMethod.setDefault(false);
            paymentMethodRepository.save(paymentMethod);
            paymentMethodRepository.findByUserEmailAndActiveTrueOrderByIsDefaultDescCreatedAtDesc(normalizedEmail)
                    .stream()
                    .findFirst()
                    .ifPresent(nextDefault -> {
                        nextDefault.setDefault(true);
                        paymentMethodRepository.save(nextDefault);
                    });
            return;
        }
        paymentMethodRepository.save(paymentMethod);
    }

    private String normalizeDetail(PaymentMethod.Type type, String detail) {
        if (type != PaymentMethod.Type.BANK_TRANSFER) {
            return null;
        }
        String normalized = detail == null ? "" : detail.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new ValidationException("Bank account details are required for Bank Transfer.");
        }
        if (normalized.length() > MAX_DETAIL_LENGTH) {
            throw new ValidationException("Bank account details are too long.");
        }
        return normalized;
    }

    private String normalize(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ValidationException("User email is required.");
        }
        return normalized;
    }
}
