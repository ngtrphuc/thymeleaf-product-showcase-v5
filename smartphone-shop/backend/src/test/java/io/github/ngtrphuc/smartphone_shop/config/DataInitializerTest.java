package io.github.ngtrphuc.smartphone_shop.config;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;

import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

import org.mockito.Mockito;

@SuppressWarnings("null")
class DataInitializerTest {

    @Test
    void initDatabase_shouldClearStorefrontCachesAfterSync() throws Exception {
        ProductRepository repository = Mockito.mock(ProductRepository.class);
        when(repository.findAll()).thenReturn(List.of());
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager("catalogPublic", "productDetailPublic");
        cacheManager.getCache("catalogPublic").put("catalog-page-1", "stale-catalog");
        cacheManager.getCache("productDetailPublic").put("product-24", "stale-detail");

        DataInitializer initializer = new DataInitializer();
        initializer.initDatabase(repository, cacheManager).run();

        verify(repository).saveAll(anyList());
        assertNull(cacheManager.getCache("catalogPublic").get("catalog-page-1"));
        assertNull(cacheManager.getCache("productDetailPublic").get("product-24"));
    }
}
