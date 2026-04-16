package io.github.ngtrphuc.smartphone_shop.controller.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;
import io.github.ngtrphuc.smartphone_shop.repository.UserRepository;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;
import io.github.ngtrphuc.smartphone_shop.service.PaymentMethodService;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private CartService cartService;

    @Mock
    private OrderService orderService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PaymentMethodService paymentMethodService;

    private CartController cartController;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        cartController = new CartController(cartService, orderService, userRepository, paymentMethodService);
        auth = new UsernamePasswordAuthenticationToken("user@example.com", "password");
        when(cartService.getCart(eq("user@example.com"), any()))
                .thenReturn(List.of(new CartItem(1L, "Phone A", 100_000.0, 1)));
    }

    @Test
    void selectPayment_shouldStoreInstallmentFromPlanChoiceAndNewMethodChoice() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String redirect = cartController.selectPayment(
                null,
                null,
                null,
                null,
                null,
                "new:MASTERCARD",
                null,
                "INSTALLMENT:24",
                null,
                auth,
                session,
                ra);

        assertEquals("redirect:/cart/shipping", redirect);
        assertEquals("MASTERCARD", session.getAttribute("paymentMethod"));
        assertNull(session.getAttribute("paymentDetail"));
        assertEquals("INSTALLMENT", session.getAttribute("paymentPlan"));
        assertEquals(24, session.getAttribute("installmentMonths"));
    }

    @Test
    void selectPayment_shouldStoreSelectedInstallmentMonthsFromPlanChoice() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String redirect = cartController.selectPayment(
                null,
                null,
                null,
                null,
                null,
                "new:PAYPAY",
                null,
                "INSTALLMENT:12",
                null,
                auth,
                session,
                ra);

        assertEquals("redirect:/cart/shipping", redirect);
        assertEquals("INSTALLMENT", session.getAttribute("paymentPlan"));
        assertEquals(12, session.getAttribute("installmentMonths"));
    }

    @Test
    void selectPayment_shouldRejectInstallmentForCashOnDelivery() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String redirect = cartController.selectPayment(
                null,
                null,
                null,
                null,
                null,
                "new:CASH_ON_DELIVERY",
                null,
                "INSTALLMENT:12",
                null,
                auth,
                session,
                ra);

        assertEquals("redirect:/cart/payment", redirect);
        assertEquals("Installment is not available for Cash on Delivery. Please choose another payment method.",
                ra.getFlashAttributes().get("toast"));
        assertNull(session.getAttribute("paymentMethod"));
    }

    @Test
    void selectPayment_shouldRejectWhenNoPaymentMethodProvided() {
        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String redirect = cartController.selectPayment(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "INSTALLMENT:24",
                null,
                auth,
                session,
                ra);

        assertEquals("redirect:/cart/payment", redirect);
        assertEquals("Please select a payment method.", ra.getFlashAttributes().get("toast"));
        assertNull(session.getAttribute("paymentMethod"));
    }

    @Test
    void selectPayment_shouldResolveSavedMethodFromChoiceWhenHiddenIdMissing() {
        PaymentMethod paymentMethod = new PaymentMethod();
        paymentMethod.setId(15L);
        paymentMethod.setType(PaymentMethod.Type.PAYPAY);
        paymentMethod.setDetail(null);
        when(paymentMethodService.getUserPaymentMethods("user@example.com"))
                .thenReturn(List.of(paymentMethod));

        MockHttpSession session = new MockHttpSession();
        RedirectAttributesModelMap ra = new RedirectAttributesModelMap();

        String redirect = cartController.selectPayment(
                null,
                null,
                null,
                "FULL_PAYMENT",
                null,
                "saved:15",
                null,
                null,
                null,
                auth,
                session,
                ra);

        assertEquals("redirect:/cart/shipping", redirect);
        assertEquals("PAYPAY", session.getAttribute("paymentMethod"));
        assertNull(session.getAttribute("paymentDetail"));
        assertEquals("FULL_PAYMENT", session.getAttribute("paymentPlan"));
        assertNull(session.getAttribute("installmentMonths"));
    }
}
