package io.github.ngtrphuc.smartphone_shop.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.anyList;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import io.github.ngtrphuc.smartphone_shop.model.CompareItemEntity;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CompareItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class CompareServiceTest {

    @Mock
    private CompareItemRepository compareItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CompareService compareService;

    @BeforeEach
    void setUp() {
        compareService = new CompareService(compareItemRepository, productRepository);
        lenient().when(productRepository.findAllByIdIn(anyList())).thenAnswer(invocation -> toProducts(invocation.getArgument(0)));
    }

    @Test
    void addItem_shouldReturnUnavailable_whenProductMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        CompareService.AddResult result = compareService.addItem("user@example.com", null, 99L);

        assertEquals(CompareService.AddResult.UNAVAILABLE, result);
        verify(compareItemRepository, never()).save(any(CompareItemEntity.class));
    }

    @Test
    void addItem_shouldNormalizeEmailPersistAndSyncSession_forLoggedInUser() {
        MockHttpSession session = new MockHttpSession();
        CompareItemEntity persisted = new CompareItemEntity("user@example.com", 10L);
        persisted.setCreatedAt(LocalDateTime.now());

        when(productRepository.existsById(10L)).thenReturn(true);
        when(compareItemRepository.existsByUserEmailAndProductId("user@example.com", 10L)).thenReturn(false);
        when(compareItemRepository.countByUserEmail("user@example.com")).thenReturn(0L);
        when(compareItemRepository.findByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(persisted));

        CompareService.AddResult result = compareService.addItem(" USER@EXAMPLE.COM ", session, 10L);

        assertEquals(CompareService.AddResult.ADDED, result);
        assertIterableEquals(List.of(10L), compareService.getCompareIds("user@example.com", session));

        ArgumentCaptor<CompareItemEntity> savedCaptor = ArgumentCaptor.forClass(CompareItemEntity.class);
        verify(compareItemRepository).save(savedCaptor.capture());
        CompareItemEntity saved = Objects.requireNonNull(savedCaptor.getValue());
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals(10L, saved.getProductId());
    }

    @Test
    void addItem_shouldReturnLimitReached_forAnonymousSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("compareIds", new java.util.ArrayList<>(List.of(1L, 2L, 3L)));
        when(productRepository.existsById(4L)).thenReturn(true);

        CompareService.AddResult result = compareService.addItem(null, session, 4L);

        assertEquals(CompareService.AddResult.LIMIT_REACHED, result);
        assertIterableEquals(List.of(1L, 2L, 3L), compareService.getCompareIds(null, session));
        verify(compareItemRepository, never()).save(any(CompareItemEntity.class));
    }

    @Test
    void saveCompareIds_shouldSanitizeAndClamp_forAnonymousSession() {
        MockHttpSession session = new MockHttpSession();

        compareService.saveCompareIds(
                null,
                session,
                java.util.Arrays.asList(3L, null, 3L, 2L, 1L, 4L));

        assertIterableEquals(List.of(3L, 2L, 1L), compareService.getCompareIds(null, session));
    }

    @Test
    void mergeSessionCompareToDb_shouldSkipDuplicatesUnavailableAndRespectLimit() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("compareIds", new java.util.ArrayList<>(List.of(2L, 3L, 4L, 5L)));

        CompareItemEntity dbLatest = new CompareItemEntity("user@example.com", 2L);
        CompareItemEntity dbOlder = new CompareItemEntity("user@example.com", 1L);
        CompareItemEntity merged = new CompareItemEntity("user@example.com", 3L);

        when(compareItemRepository.findByUserEmailOrderByCreatedAtDesc("user@example.com"))
                .thenReturn(List.of(dbLatest, dbOlder))
                .thenReturn(List.of(merged, dbLatest, dbOlder));
        when(productRepository.findAllByIdIn(anyList())).thenAnswer(invocation -> toProducts(invocation.getArgument(0)).stream()
                .filter(product -> product.getId() != 4L && product.getId() != 5L)
                .collect(Collectors.toList()));

        compareService.mergeSessionCompareToDb(session, "user@example.com");

        ArgumentCaptor<CompareItemEntity> captor = ArgumentCaptor.forClass(CompareItemEntity.class);
        verify(compareItemRepository).save(captor.capture());
        CompareItemEntity saved = Objects.requireNonNull(captor.getValue());
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals(3L, saved.getProductId());
        assertIterableEquals(List.of(3L, 2L, 1L), compareService.getCompareIds("user@example.com", session));
    }

    @Test
    void saveCompareIds_shouldDropUnavailableProducts() {
        MockHttpSession session = new MockHttpSession();
        when(productRepository.findAllByIdIn(anyList())).thenAnswer(invocation -> toProducts(invocation.getArgument(0)).stream()
                .filter(product -> product.getId() != 2L)
                .collect(Collectors.toList()));

        compareService.saveCompareIds(null, session, List.of(3L, 2L, 1L));

        assertIterableEquals(List.of(3L, 1L), compareService.getCompareIds(null, session));
    }

    @Test
    void getCompareIds_shouldRejectInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> compareService.getCompareIds("not-an-email", null));
    }

    private List<Product> toProducts(List<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream()
                .filter(id -> id != null)
                .distinct()
                .map(this::newProduct)
                .toList();
    }

    private Product newProduct(Long id) {
        Product product = new Product();
        product.setId(id);
        product.setName("Product " + id);
        product.setPrice(100.0);
        product.setStock(10);
        return product;
    }
}
