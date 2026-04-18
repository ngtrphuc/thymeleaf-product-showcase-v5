package io.github.ngtrphuc.smartphone_shop.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItemEntity;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.WishlistItemRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class WishlistServiceTest {

    @Mock
    private WishlistItemRepository wishlistItemRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private WishlistService wishlistService;

    @Test
    void addItem_shouldReturnAlreadyExistsWhenDuplicate() {
        when(productRepository.existsById(1L)).thenReturn(true);
        when(wishlistItemRepository.existsByUserEmailAndProductId("user@example.com", 1L)).thenReturn(true);

        WishlistService.AddResult result = wishlistService.addItem("USER@EXAMPLE.COM", 1L);

        assertEquals(WishlistService.AddResult.ALREADY_EXISTS, result);
        verify(wishlistItemRepository).existsByUserEmailAndProductId("user@example.com", 1L);
        verify(wishlistItemRepository, never()).deleteByUserEmailAndProductId("user@example.com", 1L);
    }

    @Test
    void getWishlist_shouldSkipOrphanProductsAndKeepOrder() {
        WishlistItemEntity latest = new WishlistItemEntity("user@example.com", 2L);
        latest.setCreatedAt(LocalDateTime.now());
        WishlistItemEntity older = new WishlistItemEntity("user@example.com", 1L);
        older.setCreatedAt(LocalDateTime.now().minusDays(1));
        WishlistItemEntity orphan = new WishlistItemEntity("user@example.com", 3L);
        orphan.setCreatedAt(LocalDateTime.now().minusDays(2));

        Product p2 = new Product();
        p2.setId(2L);
        p2.setName("Phone B");
        p2.setPrice(222.0);
        p2.setImageUrl("/images/p2.png");
        p2.setStock(9);

        Product p1 = new Product();
        p1.setId(1L);
        p1.setName("Phone A");
        p1.setPrice(111.0);
        p1.setImageUrl("/images/p1.png");
        p1.setStock(5);

        when(wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(latest, older, orphan));
        when(productRepository.findAllByIdIn(List.of(2L, 1L, 3L))).thenReturn(List.of(p2, p1));

        List<WishlistItem> result = wishlistService.getWishlist("user@example.com");

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getProductId());
        assertEquals(1L, result.get(1).getProductId());
        verify(wishlistItemRepository).findByUserEmailOrderByCreatedAtDesc("user@example.com");
        verify(productRepository).findAllByIdIn(List.of(2L, 1L, 3L));
        verify(wishlistItemRepository, never()).deleteAll(List.of(orphan));
    }

    @Test
    void cleanupOrphanedItems_shouldDeleteOnlyMissingProducts() {
        WishlistItemEntity keep = new WishlistItemEntity("user@example.com", 2L);
        WishlistItemEntity orphan = new WishlistItemEntity("user@example.com", 3L);
        Product p2 = new Product();
        p2.setId(2L);

        when(wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(keep, orphan));
        when(productRepository.findAllByIdIn(List.of(2L, 3L))).thenReturn(List.of(p2));

        wishlistService.cleanupOrphanedItems("user@example.com");

        verify(wishlistItemRepository).deleteAll(List.of(orphan));
    }

    @Test
    void cleanupOrphanedItemsForAllUsers_shouldProcessEachUser() {
        when(wishlistItemRepository.findDistinctUserEmails())
                .thenReturn(List.of("user-a@example.com", "user-b@example.com"));
        when(wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc("user-a@example.com"))
                .thenReturn(List.of());
        when(wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc("user-b@example.com"))
                .thenReturn(List.of());

        wishlistService.cleanupOrphanedItemsForAllUsers();

        verify(wishlistItemRepository).findByUserEmailOrderByCreatedAtDesc("user-a@example.com");
        verify(wishlistItemRepository).findByUserEmailOrderByCreatedAtDesc("user-b@example.com");
    }

    @Test
    void getWishlistedProductIds_shouldReturnDistinctIdsInOrder() {
        WishlistItemEntity first = new WishlistItemEntity("user@example.com", 9L);
        WishlistItemEntity second = new WishlistItemEntity("user@example.com", 5L);
        WishlistItemEntity duplicate = new WishlistItemEntity("user@example.com", 9L);

        when(wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(first, second, duplicate));

        Set<Long> ids = wishlistService.getWishlistedProductIds("user@example.com");

        assertEquals(Set.of(9L, 5L), ids);
        assertTrue(ids.contains(9L));
        assertTrue(ids.contains(5L));
    }
}
