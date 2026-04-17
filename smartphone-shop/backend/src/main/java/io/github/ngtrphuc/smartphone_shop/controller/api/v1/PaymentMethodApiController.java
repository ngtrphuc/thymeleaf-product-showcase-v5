package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;
import java.util.Locale;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;

@RestController
@RequestMapping("/api/v1/payment-methods")
public class PaymentMethodApiController {

    private final PaymentMethodService paymentMethodService;
    private final ApiMapper apiMapper;

    public PaymentMethodApiController(PaymentMethodService paymentMethodService, ApiMapper apiMapper) {
        this.paymentMethodService = paymentMethodService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    public List<PaymentMethodResponse> list(Authentication authentication) {
        return currentPaymentMethods(authentication);
    }

    @PostMapping
    public List<PaymentMethodResponse> add(@RequestBody AddPaymentMethodRequest request,
            Authentication authentication) {
        if (request.type() == null || request.type().isBlank()) {
            throw new IllegalArgumentException("Payment method type is required.");
        }
        PaymentMethod.Type paymentType = PaymentMethod.Type.valueOf(request.type().trim().toUpperCase(Locale.ROOT));
        paymentMethodService.addPaymentMethod(authentication.getName(),
                paymentType,
                request.bankDetail(),
                Boolean.TRUE.equals(request.setAsDefault()));
        return currentPaymentMethods(authentication);
    }

    @PostMapping("/{id}/default")
    public List<PaymentMethodResponse> setDefault(@PathVariable(name = "id") Long id, Authentication authentication) {
        paymentMethodService.setDefault(authentication.getName(), id);
        return currentPaymentMethods(authentication);
    }

    @DeleteMapping("/{id}")
    public List<PaymentMethodResponse> remove(@PathVariable(name = "id") Long id, Authentication authentication) {
        paymentMethodService.remove(authentication.getName(), id);
        return currentPaymentMethods(authentication);
    }

    private List<PaymentMethodResponse> currentPaymentMethods(Authentication authentication) {
        return paymentMethodService.getUserPaymentMethods(authentication.getName())
                .stream()
                .map(apiMapper::toPaymentMethodResponse)
                .toList();
    }

    private record AddPaymentMethodRequest(String type, String bankDetail, Boolean setAsDefault) {
    }
}

