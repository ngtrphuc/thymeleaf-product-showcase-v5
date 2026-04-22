package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.common.support.AssetUrlResolver;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.WishlistService;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ProductApiControllerTest {

    private static final ApiMapper API_MAPPER = new ApiMapper(new AssetUrlResolver(""));

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistService wishlistService;

    @Test
    void products_shouldFilterAppleBrandAndReturnCatalogMetadata() {
        ProductApiController controller = new ProductApiController(
                productRepository,
                wishlistService,
                API_MAPPER,
                new ConcurrentMapCacheManager("catalogPublic", "productDetailPublic"));

        Product iphone = new Product();
        iphone.setId(1L);
        iphone.setName("Apple iPhone 17 Pro");
        iphone.setPrice(249800.0);
        iphone.setStock(5);

        when(productRepository.findAll(
                ArgumentMatchers.<Specification<Product>>any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(iphone)));
        when(productRepository.findAllNamesOrdered())
                .thenReturn(List.of("Apple iPhone 17 Pro", "Samsung Galaxy S26 Ultra"));

        CatalogPageResponse response = controller.products(
                null, null, "Apple", null, null, null, null, null, null, null, null, 9, 0, null);

        assertEquals(1, response.products().size());
        assertEquals("Apple", response.products().get(0).brand());
        assertEquals(List.of("Apple", "Samsung"), response.brands());
        assertTrue(response.totalPages() >= 1);
    }

    @Test
    void products_shouldNotLeakWishlistStateAcrossUsersThroughCache() {
        ProductApiController controller = new ProductApiController(
                productRepository,
                wishlistService,
                API_MAPPER,
                new ConcurrentMapCacheManager("catalogPublic", "productDetailPublic"));

        Product iphone = new Product();
        iphone.setId(1L);
        iphone.setName("Apple iPhone 17 Pro");
        iphone.setPrice(249800.0);
        iphone.setStock(5);

        when(productRepository.findAll(ArgumentMatchers.<Specification<Product>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(iphone)));
        when(productRepository.findAllNamesOrdered()).thenReturn(List.of("Apple iPhone 17 Pro"));
        when(wishlistService.getWishlistedProductIds("alice@example.com")).thenReturn(Set.of(1L));
        when(wishlistService.getWishlistedProductIds("bob@example.com")).thenReturn(Set.of());

        CatalogPageResponse aliceResponse = controller.products(
                null, null, null, null, null, null, null, null, null, null, null, 9, 0,
                new UsernamePasswordAuthenticationToken(
                        "alice@example.com", "password", AuthorityUtils.createAuthorityList("ROLE_USER")));
        CatalogPageResponse bobResponse = controller.products(
                null, null, null, null, null, null, null, null, null, null, null, 9, 0,
                new UsernamePasswordAuthenticationToken(
                        "bob@example.com", "password", AuthorityUtils.createAuthorityList("ROLE_USER")));

        assertEquals(true, aliceResponse.products().getFirst().wishlisted());
        assertEquals(false, bobResponse.products().getFirst().wishlisted());
    }
}

