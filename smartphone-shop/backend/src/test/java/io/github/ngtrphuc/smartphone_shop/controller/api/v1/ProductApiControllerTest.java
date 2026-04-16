package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.WishlistService;

@ExtendWith(MockitoExtension.class)
class ProductApiControllerTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WishlistService wishlistService;

    @Test
    void products_shouldFilterAppleBrandAndReturnCatalogMetadata() {
        ProductApiController controller = new ProductApiController(productRepository, wishlistService, new ApiMapper());

        Product iphone = new Product();
        iphone.setId(1L);
        iphone.setName("Apple iPhone 17 Pro");
        iphone.setPrice(249800.0);
        iphone.setStock(5);

        Product galaxy = new Product();
        galaxy.setId(2L);
        galaxy.setName("Samsung Galaxy S26 Ultra");
        galaxy.setPrice(299200.0);
        galaxy.setStock(7);

        when(productRepository.findWithFilters(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(iphone, galaxy)));
        when(productRepository.findAllNamesOrdered())
                .thenReturn(List.of("Apple iPhone 17 Pro", "Samsung Galaxy S26 Ultra"));

        CatalogPageResponse response = controller.products(
                null, null, "Apple", null, null, null, null, null, null, null, 9, 0, null);

        assertEquals(1, response.products().size());
        assertEquals("Apple", response.products().get(0).brand());
        assertEquals(List.of("Apple", "Samsung"), response.brands());
        assertTrue(response.totalPages() >= 1);
    }
}

