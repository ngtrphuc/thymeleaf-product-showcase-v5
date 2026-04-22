package io.github.ngtrphuc.smartphone_shop.service;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import io.github.ngtrphuc.smartphone_shop.model.CompareItemEntity;
import io.github.ngtrphuc.smartphone_shop.repository.CompareItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class CompareServiceTest {

    @Mock
    private CompareItemRepository compareItemRepository;

    @Mock
    private ProductRepository productRepository;

    private CompareService compareService;

    @BeforeEach
    void setUp() {
        compareService = new CompareService(compareItemRepository, productRepository);
    }

    @Test
    void addItem_shouldReturnUnavailable_whenProductMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        CompareService.AddResult result = compareService.addItem("user@example.com", null, 99L);

        assertEquals(CompareService.AddResult.UNAVAILABLE, result);
        verify(compareItemRepository, never()).save(MockitoNullSafety.anyNonNull(CompareItemEntity.class));
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
        verify(compareItemRepository).save(MockitoNullSafety.captureNonNull(savedCaptor));
        CompareItemEntity saved = MockitoNullSafety.capturedValue(savedCaptor);
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals(10L, saved.getProductId());
    }

    @Test
    void addItem_shouldReturnLimitReached_forAnonymousSession() {
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("compareIds", new java.util.ArrayList<>(List.of(1L, 2L, 3L)));
        when(productRepository.existsById(4L)).thenReturn(true);
        when(productRepository.existsById(1L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(true);
        when(productRepository.existsById(3L)).thenReturn(true);

        CompareService.AddResult result = compareService.addItem(null, session, 4L);

        assertEquals(CompareService.AddResult.LIMIT_REACHED, result);
        assertIterableEquals(List.of(1L, 2L, 3L), compareService.getCompareIds(null, session));
        verify(compareItemRepository, never()).save(MockitoNullSafety.anyNonNull(CompareItemEntity.class));
    }

    @Test
    void saveCompareIds_shouldSanitizeAndClamp_forAnonymousSession() {
        MockHttpSession session = new MockHttpSession();
        when(productRepository.existsById(anyLong())).thenReturn(true);

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
        when(productRepository.existsById(anyLong())).thenAnswer(invocation -> {
            Long id = invocation.getArgument(0, Long.class);
            return id != null && id != 4L && id != 5L;
        });

        compareService.mergeSessionCompareToDb(session, "user@example.com");

        ArgumentCaptor<CompareItemEntity> captor = ArgumentCaptor.forClass(CompareItemEntity.class);
        verify(compareItemRepository).save(MockitoNullSafety.captureNonNull(captor));
        CompareItemEntity saved = MockitoNullSafety.capturedValue(captor);
        assertEquals("user@example.com", saved.getUserEmail());
        assertEquals(3L, saved.getProductId());
        assertIterableEquals(List.of(3L, 2L, 1L), compareService.getCompareIds("user@example.com", session));
    }

    @Test
    void saveCompareIds_shouldDropUnavailableProducts() {
        MockHttpSession session = new MockHttpSession();
        when(productRepository.existsById(3L)).thenReturn(true);
        when(productRepository.existsById(2L)).thenReturn(false);
        when(productRepository.existsById(1L)).thenReturn(true);

        compareService.saveCompareIds(null, session, List.of(3L, 2L, 1L));

        assertIterableEquals(List.of(3L, 1L), compareService.getCompareIds(null, session));
    }

    @Test
    void getCompareIds_shouldRejectInvalidEmail() {
        assertThrows(IllegalArgumentException.class,
                () -> compareService.getCompareIds("not-an-email", null));
    }
}
