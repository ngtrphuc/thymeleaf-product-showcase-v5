package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.*;
import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.spec.ProductCatalogSpecifications;
import io.github.ngtrphuc.smartphone_shop.common.support.StorefrontSupport;
import io.github.ngtrphuc.smartphone_shop.service.WishlistService;

@RestController
@RequestMapping("/api/v1/products")
public class ProductApiController {

    private static final int DESKTOP_PAGE_SIZE = 9;
    private static final int COMPACT_PAGE_SIZE = 8;

    private final ProductRepository productRepository;
    private final WishlistService wishlistService;
    private final ApiMapper apiMapper;

    public ProductApiController(ProductRepository productRepository,
            WishlistService wishlistService,
            ApiMapper apiMapper) {
        this.productRepository = productRepository;
        this.wishlistService = wishlistService;
        this.apiMapper = apiMapper;
    }

    @GetMapping
    @Cacheable(
            value = "catalogPublic",
            key = "T(io.github.ngtrphuc.smartphone_shop.common.support.CacheKeys).catalog(#keyword,#sort,#brand,#priceRange,#priceMin,#priceMax,#batteryRange,#batteryMin,#batteryMax,#screenSize,#pageSize,#page)",
            condition = "#authentication == null")
    public CatalogPageResponse products(
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "priceRange", required = false) String priceRange,
            @RequestParam(name = "priceMin", required = false) Double priceMin,
            @RequestParam(name = "priceMax", required = false) Double priceMax,
            @RequestParam(name = "batteryRange", required = false) String batteryRange,
            @RequestParam(name = "batteryMin", required = false) Integer batteryMin,
            @RequestParam(name = "batteryMax", required = false) Integer batteryMax,
            @RequestParam(name = "screenSize", required = false) String screenSize,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(name = "page", defaultValue = "0") int page,
            Authentication authentication) {

        Double resolvedPriceMin = priceMin;
        Double resolvedPriceMax = priceMax;
        if (priceRange != null) {
            switch (priceRange) {
                case "under150" -> resolvedPriceMax = resolvedPriceMax == null ? 149999.0 : resolvedPriceMax;
                case "150to200" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 150000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 199999.0);
                }
                case "200to250" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 200000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 250000.0);
                }
                case "over250" -> resolvedPriceMin = resolveMin(resolvedPriceMin, 250001.0);
                default -> {
                }
            }
        }

        int effectivePageSize = resolvePageSize(pageSize);
        int safeRequestedPage = Math.max(page, 0);
        String normalizedSort = normalizeSort(sort);
        Sort requestedSort = switch (normalizedSort) {
            case "name_desc" -> Sort.by(Sort.Order.desc("name").ignoreCase());
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            default -> Sort.by(Sort.Order.asc("name").ignoreCase());
        };
        List<Product> products;
        long totalElements;
        int totalPages;
        int safePage;
        int activeFilterCount = countActiveFilters(
                keyword, brand, priceRange, priceMin, priceMax, batteryRange, batteryMin, batteryMax, screenSize);

        Page<Product> productPage = productRepository.findAll(
                ProductCatalogSpecifications.forCatalog(
                        blankToNull(keyword),
                        resolvedPriceMin,
                        resolvedPriceMax,
                        blankToNull(brand),
                        blankToNull(batteryRange),
                        batteryMin,
                        batteryMax,
                        blankToNull(screenSize)),
                PageRequest.of(safeRequestedPage, effectivePageSize, requestedSort));
        if (productPage.isEmpty() && safeRequestedPage > 0 && productPage.getTotalPages() > 0) {
            productPage = productRepository.findAll(
                    ProductCatalogSpecifications.forCatalog(
                            blankToNull(keyword),
                            resolvedPriceMin,
                            resolvedPriceMax,
                            blankToNull(brand),
                            blankToNull(batteryRange),
                            batteryMin,
                            batteryMax,
                            blankToNull(screenSize)),
                    PageRequest.of(productPage.getTotalPages() - 1, effectivePageSize, requestedSort));
        }
        products = productPage.getContent();
        totalElements = productPage.getTotalElements();
        totalPages = Math.max(productPage.getTotalPages(), 1);
        safePage = Math.max(0, Math.min(productPage.getNumber(), totalPages - 1));

        List<String> brands = resolveAvailableBrands();
        Set<Long> wishlistedProductIds = resolveWishlistedProductIds(authentication);
        return new CatalogPageResponse(
                products.stream()
                        .map(product -> apiMapper.toProductSummary(
                                product,
                                product != null && product.getId() != null && wishlistedProductIds.contains(product.getId())))
                        .toList(),
                safePage,
                totalPages,
                totalElements,
                effectivePageSize,
                brands,
                activeFilterCount,
                activeFilterCount > 0);
    }

    @GetMapping("/{id}")
    @Cacheable(value = "productDetailPublic", key = "#id", condition = "#authentication == null")
    public ProductDetailResponse product(@PathVariable(name = "id") long id, Authentication authentication) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found."));
        Set<Long> wishlistedProductIds = resolveWishlistedProductIds(authentication);
        boolean wishlisted = wishlistedProductIds.contains(id);
        List<ProductSummary> recommendedProducts = resolveRecommendedProducts(product).stream()
                .map(recommended -> apiMapper.toProductSummary(
                        recommended,
                        recommended != null && recommended.getId() != null
                                && wishlistedProductIds.contains(recommended.getId())))
                .toList();
        return new ProductDetailResponse(
                apiMapper.toProductSummary(product, wishlisted),
                recommendedProducts,
                wishlisted);
    }

    private List<Product> resolveRecommendedProducts(Product currentProduct) {
        if (currentProduct == null || currentProduct.getId() == null) {
            return List.of();
        }
        double targetPrice = currentProduct.getPrice() == null ? 0.0 : currentProduct.getPrice();
        return productRepository.findRecommendedProducts(
                currentProduct.getId(),
                targetPrice,
                PageRequest.of(0, 4));
    }

    private List<String> resolveAvailableBrands() {
        return productRepository.findAllNamesOrdered().stream()
                .map(StorefrontSupport::extractBrand)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private Set<Long> resolveWishlistedProductIds(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Set.of();
        }
        return wishlistService.getWishlistedProductIds(authentication.getName());
    }

    private int countActiveFilters(String keyword, String brand, String priceRange,
            Double priceMin, Double priceMax, String batteryRange, Integer batteryMin,
            Integer batteryMax, String screenSize) {
        int count = 0;
        if (keyword != null && !keyword.isBlank()) count++;
        if (brand != null && !brand.isBlank()) count++;
        if (priceRange != null && !priceRange.isBlank()) count++;
        if (priceMin != null || priceMax != null) count++;
        if (batteryRange != null && !batteryRange.isBlank()) count++;
        if (batteryMin != null || batteryMax != null) count++;
        if (screenSize != null && !screenSize.isBlank()) count++;
        return count;
    }

    private int resolvePageSize(Integer pageSize) {
        return pageSize != null && pageSize == COMPACT_PAGE_SIZE ? COMPACT_PAGE_SIZE : DESKTOP_PAGE_SIZE;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "name_asc";
        }
        return switch (sort) {
            case "name_desc", "price_asc", "price_desc" -> sort;
            default -> "name_asc";
        };
    }

    private Double resolveMin(Double existing, Double fallback) {
        return existing != null ? existing : fallback;
    }

    private Double resolveMax(Double existing, Double fallback) {
        return existing != null ? existing : fallback;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}


