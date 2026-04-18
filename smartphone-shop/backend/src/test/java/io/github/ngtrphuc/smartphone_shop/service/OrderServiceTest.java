package io.github.ngtrphuc.smartphone_shop.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, productRepository);
    }

    @Test
    void updateStatus_shouldThrowWhenOrderNotFound() {
        when(orderRepository.findByIdWithItemsForUpdate(99L)).thenReturn(Optional.empty());

        assertThrows(OrderValidationException.class, () -> orderService.updateStatus(99L, "processing"));
        verify(orderRepository).findByIdWithItemsForUpdate(99L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void updateStatus_shouldThrowForUnsupportedStatus() {
        Order order = new Order();
        order.setId(7L);
        order.setStatus("pending");
        when(orderRepository.findByIdWithItemsForUpdate(7L)).thenReturn(Optional.of(order));

        assertThrows(OrderValidationException.class, () -> orderService.updateStatus(7L, "invalid_status"));
        verify(orderRepository).findByIdWithItemsForUpdate(7L);
        verifyNoMoreInteractions(orderRepository);
    }

    @Test
    void createOrder_shouldRejectInvalidPhoneFormat() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Phone A");
        product.setPrice(100.0);
        product.setStock(5);

        CartItem item = new CartItem(1L, "Phone A", 100.0, 1);
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(product));

        assertThrows(OrderValidationException.class, () ->
                orderService.createOrder("user@example.com", "John Doe", "abc123", "Tokyo", List.of(item)));
    }

    @Test
    void createOrder_shouldRecalculatePricesFromCurrentCatalog() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Phone A Updated");
        product.setPrice(250.0);
        product.setStock(5);

        CartItem staleCartItem = new CartItem(1L, "Phone A", 100.0, 2);
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(product));
        when(orderRepository.save(MockitoNullSafety.anyNonNull(Order.class)))
                .thenAnswer(MockitoNullSafety.returnsFirstArgument(Order.class));

        Order created = orderService.createOrder(
                "user@example.com", "John Doe", "0901234567", "Tokyo", List.of(staleCartItem));

        assertEquals(500.0, created.getTotalAmount());
        assertEquals("Phone A Updated", created.getItems().getFirst().getProductName());
        assertEquals(250.0, created.getItems().getFirst().getPrice());
        assertEquals(3, product.getStock());
    }

    @Test
    void createOrder_shouldRejectInvalidPaymentMethod() {
        CartItem item = new CartItem(1L, "Phone A", 100.0, 1);

        assertThrows(OrderValidationException.class, () ->
                orderService.createOrder(
                        "user@example.com",
                        "John Doe",
                        "0901234567",
                        "Tokyo",
                        List.of(item),
                        "CRYPTO",
                        null));
    }

    @Test
    void createOrder_shouldAcceptMasterCardPaymentMethod() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Phone A");
        product.setPrice(100.0);
        product.setStock(5);

        CartItem item = new CartItem(1L, "Phone A", 100.0, 1);
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(product));
        when(orderRepository.save(MockitoNullSafety.anyNonNull(Order.class)))
                .thenAnswer(MockitoNullSafety.returnsFirstArgument(Order.class));

        Order created = orderService.createOrder(
                "user@example.com",
                "John Doe",
                "0901234567",
                "Tokyo",
                List.of(item),
                "MASTERCARD",
                null);

        assertEquals("MASTERCARD", created.getPaymentMethod());
        assertEquals(null, created.getPaymentDetail());
    }

    @Test
    void createOrder_shouldStoreInstallmentPlanAndMonthlyAmount() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Phone A");
        product.setPrice(240000.0);
        product.setStock(5);

        CartItem item = new CartItem(1L, "Phone A", 240000.0, 1);
        when(productRepository.findAllByIdInForUpdate(anyCollection())).thenReturn(List.of(product));
        when(orderRepository.save(MockitoNullSafety.anyNonNull(Order.class)))
                .thenAnswer(MockitoNullSafety.returnsFirstArgument(Order.class));

        Order created = orderService.createOrder(
                "user@example.com",
                "John Doe",
                "0901234567",
                "Tokyo",
                List.of(item),
                "MASTERCARD",
                null,
                "INSTALLMENT",
                24);

        assertEquals("INSTALLMENT", created.getPaymentPlan());
        assertEquals(24, created.getInstallmentMonths());
        assertEquals(10000L, created.getInstallmentMonthlyAmount());
    }

    @Test
    void createOrder_shouldRejectUnsupportedInstallmentPeriod() {
        CartItem item = new CartItem(1L, "Phone A", 100.0, 1);

        assertThrows(OrderValidationException.class, () -> orderService.createOrder(
                "user@example.com",
                "John Doe",
                "0901234567",
                "Tokyo",
                List.of(item),
                "MASTERCARD",
                null,
                "INSTALLMENT",
                18));
    }

    @Test
    void createOrder_shouldRejectInstallmentForCashOnDelivery() {
        CartItem item = new CartItem(1L, "Phone A", 100.0, 1);

        assertThrows(OrderValidationException.class, () -> orderService.createOrder(
                "user@example.com",
                "John Doe",
                "0901234567",
                "Tokyo",
                List.of(item),
                "CASH_ON_DELIVERY",
                null,
                "INSTALLMENT",
                12));
    }
}
