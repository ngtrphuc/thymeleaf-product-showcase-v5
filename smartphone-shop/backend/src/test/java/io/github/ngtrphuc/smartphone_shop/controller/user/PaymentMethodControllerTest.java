package io.github.ngtrphuc.smartphone_shop.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.ngtrphuc.smartphone_shop.common.exception.ResourceNotFoundException;
import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;

@ExtendWith(MockitoExtension.class)
class PaymentMethodControllerTest {

    @Mock
    private PaymentMethodService paymentMethodService;

    private PaymentMethodController paymentMethodController;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        paymentMethodController = new PaymentMethodController(paymentMethodService);
        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password");
    }

    @Test
    void add_shouldFlashValidationMessageWhenServiceRejectsInput() {
        doThrow(new ValidationException("Bank account details are required for Bank Transfer."))
                .when(paymentMethodService)
                .addPaymentMethod("user@example.com",
                        io.github.ngtrphuc.smartphone_shop.model.PaymentMethod.Type.BANK_TRANSFER,
                        "",
                        false);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = paymentMethodController.add("bank_transfer", "", false, auth, redirectAttributes);

        assertEquals("redirect:/profile", view);
        assertEquals("Bank account details are required for Bank Transfer.",
                redirectAttributes.getFlashAttributes().get("toast"));
    }

    @Test
    void setDefault_shouldFlashMessageWhenPaymentMethodDoesNotExist() {
        doThrow(new ResourceNotFoundException("Payment method not found."))
                .when(paymentMethodService)
                .setDefault("user@example.com", 99L);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = paymentMethodController.setDefault(99L, auth, redirectAttributes);

        assertEquals("redirect:/profile", view);
        assertEquals("Payment method not found.", redirectAttributes.getFlashAttributes().get("toast"));
    }

    @Test
    void remove_shouldFlashMessageWhenPaymentMethodDoesNotExist() {
        doThrow(new ResourceNotFoundException("Payment method not found."))
                .when(paymentMethodService)
                .remove("user@example.com", 77L);

        RedirectAttributesModelMap redirectAttributes = new RedirectAttributesModelMap();
        String view = paymentMethodController.remove(77L, auth, redirectAttributes);

        assertEquals("redirect:/profile", view);
        assertEquals("Payment method not found.", redirectAttributes.getFlashAttributes().get("toast"));
    }
}
