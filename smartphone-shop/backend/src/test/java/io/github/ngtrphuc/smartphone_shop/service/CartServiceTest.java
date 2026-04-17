package io.github.ngtrphuc.smartphone_shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.CartItemEntity;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CartServiceTest {

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CartService cartService;

    @BeforeEach
    void setUp() {
        cartService = new CartService(cartItemRepository, productRepository);
    }

    @Test
    void mergeSessionCartToDb_shouldClampByStockAndSkipOutOfStock() {
        MockHttpSession session = new MockHttpSession();
        List<CartItem> sessionCart = new java.util.ArrayList<>();
        sessionCart.add(new CartItem(1L, "Phone A", 100.0, 5));
        sessionCart.add(new CartItem(2L, "Phone B", 200.0, 2));
        session.setAttribute("cart", sessionCart);

        Product inStock = new Product();
        inStock.setId(1L);
        inStock.setStock(3);

        Product outOfStock = new Product();
        outOfStock.setId(2L);
        outOfStock.setStock(0);

        CartItemEntity existing = new CartItemEntity("user@example.com", 1L, 1);

        when(productRepository.findAllByIdIn(any())).thenReturn(List.of(inStock, outOfStock));
        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(List.of(existing));

        cartService.mergeSessionCartToDb(session, "user@example.com");

        verify(cartItemRepository).save(existing);
        assertEquals(Long.valueOf(1L), existing.getProductId());
        assertEquals(3, existing.getQuantity());
        assertNull(session.getAttribute("cart"));
        assertEquals(3, session.getAttribute("cartCount"));
    }

    @Test
    void increaseItem_shouldDoNothingWhenProductMissingOrNoStock() {
        MockHttpSession session = new MockHttpSession();
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        cartService.increaseItem("user@example.com", session, 10L);

        verify(cartItemRepository, never()).findByUserEmailAndProductId(any(), any());
    }

    @Test
    void getDbCart_shouldSkipOrphanedItemsWithoutRepairWrites() {
        CartItemEntity orphan = new CartItemEntity("user@example.com", 99L, 2);
        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(List.of(orphan));
        when(productRepository.findAllByIdIn(List.of(99L))).thenReturn(List.of());

        List<CartItem> cart = cartService.getDbCart("user@example.com");

        assertTrue(cart.isEmpty());
        verify(cartItemRepository, never()).deleteAll(anyIterable());
    }

    @Test
    void getDbCart_shouldClampQuantityInResponseWithoutPersisting() {
        CartItemEntity entity = new CartItemEntity("user@example.com", 7L, 5);
        Product product = new Product();
        product.setId(7L);
        product.setName("Phone C");
        product.setPrice(300.0);
        product.setStock(2);

        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(List.of(entity));
        when(productRepository.findAllByIdIn(List.of(7L))).thenReturn(List.of(product));

        List<CartItem> cart = cartService.getDbCart("user@example.com");

        assertEquals(1, cart.size());
        assertEquals(2, cart.getFirst().getQuantity());
        verify(cartItemRepository, never()).saveAll(anyIterable());
    }

    @Test
    void cleanupDbCart_shouldRemoveOrphanedAndClampQuantities() {
        CartItemEntity orphan = new CartItemEntity("user@example.com", 99L, 2);
        CartItemEntity limited = new CartItemEntity("user@example.com", 7L, 5);
        Product product = new Product();
        product.setId(7L);
        product.setStock(2);

        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(List.of(orphan, limited));
        when(productRepository.findAllByIdIn(List.of(99L, 7L))).thenReturn(List.of(product));

        cartService.cleanupDbCart("user@example.com");

        assertEquals(2, limited.getQuantity());
        verify(cartItemRepository).deleteAll(MockitoNullSafety.nonNullIterable(List.of(orphan)));
        verify(cartItemRepository).saveAll(MockitoNullSafety.nonNullIterable(List.of(limited)));
    }

    @Test
    void syncCartCount_shouldUseSnapshotWithoutRepairWrites() {
        MockHttpSession session = new MockHttpSession();
        CartItemEntity entity = new CartItemEntity("user@example.com", 7L, 5);
        Product product = new Product();
        product.setId(7L);
        product.setName("Phone Snapshot");
        product.setPrice(300.0);
        product.setStock(2);

        when(cartItemRepository.findByUserEmail("user@example.com")).thenReturn(List.of(entity));
        when(productRepository.findAllByIdIn(List.of(7L))).thenReturn(List.of(product));

        cartService.syncCartCount(session, "user@example.com");

        assertEquals(2, session.getAttribute("cartCount"));
        verify(cartItemRepository, never()).saveAll(anyIterable());
        verify(cartItemRepository, never()).deleteAll(anyIterable());
    }

    @Test
    void addItem_shouldRespectRequestedQuantityForSessionCartAndClampByStock() {
        MockHttpSession session = new MockHttpSession();
        Product product = new Product();
        product.setId(5L);
        product.setName("Phone D");
        product.setPrice(500.0);
        product.setStock(4);

        when(productRepository.findById(5L)).thenReturn(Optional.of(product));

        CartService.AddItemResult first = cartService.addItem(null, session, 5L, 3);
        CartService.AddItemResult second = cartService.addItem(null, session, 5L, 5);
        CartService.AddItemResult third = cartService.addItem(null, session, 5L, 1);

        List<CartItem> cart = cartService.getSessionCart(session);
        assertEquals(CartService.AddItemResult.ADDED, first);
        assertEquals(CartService.AddItemResult.ADDED, second);
        assertEquals(CartService.AddItemResult.LIMIT_REACHED, third);
        assertEquals(1, cart.size());
        assertEquals(4, cart.getFirst().getQuantity());
    }

    @Test
    void addItem_shouldRespectRequestedQuantityForDbCartAndClampByStock() {
        MockHttpSession session = new MockHttpSession();
        Product product = new Product();
        product.setId(8L);
        product.setName("Phone E");
        product.setPrice(800.0);
        product.setStock(6);
        CartItemEntity existing = new CartItemEntity("user@example.com", 8L, 2);

        when(productRepository.findById(8L)).thenReturn(Optional.of(product));
        when(cartItemRepository.findByUserEmailAndProductId("user@example.com", 8L))
                .thenReturn(Optional.of(existing));

        CartService.AddItemResult first = cartService.addItem("user@example.com", session, 8L, 4);
        CartService.AddItemResult second = cartService.addItem("user@example.com", session, 8L, 1);

        assertEquals(CartService.AddItemResult.ADDED, first);
        assertEquals(CartService.AddItemResult.LIMIT_REACHED, second);
        assertEquals(6, existing.getQuantity());
        verify(cartItemRepository).save(existing);
    }
}
