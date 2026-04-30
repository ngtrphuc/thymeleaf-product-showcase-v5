package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.common.exception.UnauthorizedActionException;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderItem;
import io.github.ngtrphuc.smartphone_shop.service.CartService;
import io.github.ngtrphuc.smartphone_shop.service.OrderIdempotencyService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CartService cartService;

    @MockitoBean
    private OrderIdempotencyService orderIdempotencyService;

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void place_shouldReturnValidationFailed_whenCustomerNameBlank() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", "checkout-key-invalid")
                .contentType("application/json")
                .content("""
                        {
                          "customerName": "   ",
                          "phoneNumber": "0901234567",
                          "shippingAddress": "Tokyo",
                          "paymentMethod": "CASH_ON_DELIVERY",
                          "paymentPlan": "FULL_PAYMENT"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.message").value("Customer name is required."));

        verify(orderIdempotencyService, never()).executeCheckout(any(), any(), any(), any());
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void place_shouldCreateOrder_whenRequestIsValid() throws Exception {
        CartItem cartItem = new CartItem(1L, "Phone A", 100.0, 1);
        when(cartService.getUserCart("user@example.com")).thenReturn(List.of(cartItem));
        when(orderIdempotencyService.createFingerprint(
                eq("user@example.com"),
                eq("Nguyen Phuc"),
                eq("0901234567"),
                eq("Tokyo"),
                eq("CASH_ON_DELIVERY"),
                eq(null),
                eq("FULL_PAYMENT"),
                eq(null)))
                .thenReturn("fingerprint-1");

        Order created = buildOrder();
        when(orderIdempotencyService.executeCheckout(
                eq("user@example.com"),
                eq("checkout-key-1"),
                eq("fingerprint-1"),
                any()))
                .thenAnswer(this::runOrderCreator);
        when(orderService.createOrder(
                eq("user@example.com"),
                eq("Nguyen Phuc"),
                eq("0901234567"),
                eq("Tokyo"),
                any(),
                eq("CASH_ON_DELIVERY"),
                eq(null),
                eq("FULL_PAYMENT"),
                eq(null)))
                .thenReturn(created);

        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", "checkout-key-1")
                .contentType("application/json")
                .content("""
                        {
                          "customerName": "Nguyen Phuc",
                          "phoneNumber": "0901234567",
                          "shippingAddress": "Tokyo",
                          "paymentMethod": "CASH_ON_DELIVERY",
                          "paymentPlan": "FULL_PAYMENT"
                        }
                        """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.itemCount").value(1));

        verify(cartService).clearCart("user@example.com", null);
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void place_shouldReturnBadRequest_whenCartIsEmpty() throws Exception {
        when(cartService.getUserCart("user@example.com")).thenReturn(List.of());
        when(orderIdempotencyService.createFingerprint(
                eq("user@example.com"),
                eq("Nguyen Phuc"),
                eq("0901234567"),
                eq("Tokyo"),
                eq("CASH_ON_DELIVERY"),
                eq(null),
                eq("FULL_PAYMENT"),
                eq(null)))
                .thenReturn("fingerprint-2");
        when(orderIdempotencyService.executeCheckout(
                eq("user@example.com"),
                eq("checkout-key-2"),
                eq("fingerprint-2"),
                any()))
                .thenAnswer(this::runOrderCreator);
        when(orderService.createOrder(
                eq("user@example.com"),
                eq("Nguyen Phuc"),
                eq("0901234567"),
                eq("Tokyo"),
                any(),
                eq("CASH_ON_DELIVERY"),
                eq(null),
                eq("FULL_PAYMENT"),
                eq(null)))
                .thenThrow(new OrderValidationException("Your cart is empty."));

        mockMvc.perform(post("/api/v1/orders")
                .header("Idempotency-Key", "checkout-key-2")
                .contentType("application/json")
                .content("""
                        {
                          "customerName": "Nguyen Phuc",
                          "phoneNumber": "0901234567",
                          "shippingAddress": "Tokyo",
                          "paymentMethod": "CASH_ON_DELIVERY",
                          "paymentPlan": "FULL_PAYMENT"
                        }
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORDER_VALIDATION_FAILED"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void place_shouldReturnBadRequest_whenIdempotencyHeaderIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content("""
                        {
                          "customerName": "Nguyen Phuc",
                          "phoneNumber": "0901234567",
                          "shippingAddress": "Tokyo",
                          "paymentMethod": "CASH_ON_DELIVERY",
                          "paymentPlan": "FULL_PAYMENT"
                        }
                        """))
                .andExpect(status().isBadRequest());

        verify(orderIdempotencyService, never()).executeCheckout(any(), any(), any(), any());
    }

    @Test
    void place_shouldReturnUnauthorized_whenNotAuthenticated() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType("application/json")
                .content("""
                        {
                          "customerName": "Nguyen Phuc",
                          "phoneNumber": "0901234567",
                          "shippingAddress": "Tokyo",
                          "paymentMethod": "CASH_ON_DELIVERY",
                          "paymentPlan": "FULL_PAYMENT"
                        }
                        """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    @WithMockUser(username = "user@example.com", roles = "USER")
    void cancel_shouldReturnForbidden_whenOrderBelongsToAnotherUser() throws Exception {
        when(orderService.cancelOrder(99L, "user@example.com"))
                .thenThrow(new UnauthorizedActionException("You do not have permission to cancel this order."));

        mockMvc.perform(post("/api/v1/orders/99/cancel"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private Order runOrderCreator(InvocationOnMock invocation) {
        Supplier<?> supplier = invocation.getArgument(3);
        return (Order) supplier.get();
    }

    private Order buildOrder() {
        Order order = new Order();
        order.setId(10L);
        order.setUserEmail("user@example.com");
        order.setCustomerName("Nguyen Phuc");
        order.setPhoneNumber("0901234567");
        order.setShippingAddress("Tokyo");
        order.setPaymentMethod("CASH_ON_DELIVERY");
        order.setPaymentPlan("FULL_PAYMENT");
        order.setStatus("pending");
        order.setTotalAmount(100.0);
        order.setCreatedAt(LocalDateTime.of(2026, 4, 18, 12, 0));

        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProductId(1L);
        item.setProductName("Phone A");
        item.setPrice(100.0);
        item.setQuantity(1);
        order.setItems(new java.util.ArrayList<>(List.of(item)));
        return order;
    }
}
