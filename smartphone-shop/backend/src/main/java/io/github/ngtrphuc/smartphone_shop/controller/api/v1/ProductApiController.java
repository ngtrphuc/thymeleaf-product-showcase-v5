package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.CatalogFilterRequest;
import io.github.ngtrphuc.smartphone_shop.api.dto.CatalogPageResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductDetailResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductImageResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductSpecResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductSummary;
import io.github.ngtrphuc.smartphone_shop.api.dto.ProductVariantResponse;
import io.github.ngtrphuc.smartphone_shop.common.support.CacheKeys;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.spec.ProductCatalogSpecifications;
import io.github.ngtrphuc.smartphone_shop.service.ProductCommerceService;
import io.github.ngtrphuc.smartphone_shop.service.ProductSearchService;
import io.github.ngtrphuc.smartphone_shop.service.WishlistService;

@RestController
@RequestMapping("/api/v1/products")
public class ProductApiController {
    private static final Logger log = LoggerFactory.getLogger(ProductApiController.class);

    private static final int DESKTOP_PAGE_SIZE = 9;
    private static final int COMPACT_PAGE_SIZE = 8;
    private static final String CATALOG_PUBLIC_CACHE = "catalogPublic";
    private static final String PRODUCT_DETAIL_PUBLIC_CACHE = "productDetailPublic";
    private static final String BRAND_LIST_CACHE = "brandList";

    private final ProductRepository productRepository;
    private final WishlistService wishlistService;
    private final ApiMapper apiMapper;
    private final CacheManager cacheManager;
    private final ProductCommerceService productCommerceService;
    private ProductSearchService productSearchService;

    @Autowired
    public ProductApiController(ProductRepository productRepository,
            WishlistService wishlistService,
            ApiMapper apiMapper,
            CacheManager cacheManager,
            ProductCommerceService productCommerceService) {
        this.productRepository = productRepository;
        this.wishlistService = wishlistService;
        this.apiMapper = apiMapper;
        this.cacheManager = cacheManager;
        this.productCommerceService = productCommerceService;
    }

    /**
     * Backward-compatible constructor for existing tests.
     */
    public ProductApiController(ProductRepository productRepository,
            WishlistService wishlistService,
            ApiMapper apiMapper,
            CacheManager cacheManager) {
        this(productRepository, wishlistService, apiMapper, cacheManager,
                new ProductCommerceService(null, null, null, null, null));
    }

    @Autowired(required = false)
    void setProductSearchService(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping
    public CatalogPageResponse products(
            @ModelAttribute CatalogFilterRequest filter,
            Authentication authentication) {
        String cacheKey = Objects.requireNonNull(CacheKeys.catalog(
                filter.keyword(),
                filter.sort(),
                filter.brand(),
                filter.storage(),
                filter.priceRange(),
                filter.priceMin(),
                filter.priceMax(),
                filter.batteryRange(),
                filter.batteryMin(),
                filter.batteryMax(),
                filter.screenSize(),
                filter.pageSize(),
                filter.resolvedPage()));
        CatalogPageResponse publicResponse = getOrLoadCache(
                CATALOG_PUBLIC_CACHE,
                cacheKey,
                () -> buildCatalogPublicResponse(
                        filter.keyword(),
                        filter.sort(),
                        filter.brand(),
                        filter.storage(),
                        filter.priceRange(),
                        filter.priceMin(),
                        filter.priceMax(),
                        filter.batteryRange(),
                        filter.batteryMin(),
                        filter.batteryMax(),
                        filter.screenSize(),
                        filter.pageSize(),
                        filter.resolvedPage()));
        publicResponse = normalizeCatalogPageResponse(publicResponse);

        Set<Long> wishlistedProductIds = resolveWishlistedProductIds(authentication);
        if (wishlistedProductIds.isEmpty()) {
            return publicResponse;
        }
        return applyWishlistToCatalog(publicResponse, wishlistedProductIds);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse product(
            @PathVariable(name = "id") long id,
            @RequestParam(name = "variantId", required = false) Long variantId,
            Authentication authentication) {
        String detailKey = Objects.requireNonNull(CacheKeys.productDetail(id));
        ProductDetailResponse publicResponse = getOrLoadCache(
                PRODUCT_DETAIL_PUBLIC_CACHE,
                detailKey,
                () -> buildProductDetailPublicResponse(id, null));
        publicResponse = normalizeProductDetailResponse(publicResponse);

        ProductDetailResponse resolvedResponse = publicResponse;
        if (variantId != null) {
            resolvedResponse = buildProductDetailPublicResponse(id, variantId);
        }
        resolvedResponse = normalizeProductDetailResponse(resolvedResponse);

        Set<Long> wishlistedProductIds = resolveWishlistedProductIds(authentication);
        if (wishlistedProductIds.isEmpty()) {
            return resolvedResponse;
        }
        return applyWishlistToProductDetail(resolvedResponse, wishlistedProductIds);
    }

    private CatalogPageResponse buildCatalogPublicResponse(
            String keyword,
            String sort,
            String brand,
            String storage,
            String priceRange,
            Double priceMin,
            Double priceMax,
            String batteryRange,
            Integer batteryMin,
            Integer batteryMax,
            String screenSize,
            Integer pageSize,
            int page) {
        Double resolvedPriceMin = priceMin;
        Double resolvedPriceMax = priceMax;
        if (priceRange != null) {
            switch (priceRange) {
                case "under150" -> resolvedPriceMax = resolvedPriceMax == null ? 149999.0 : resolvedPriceMax;
                case "150to199" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 150000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 199999.0);
                }
                case "200to249" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 200000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 249999.0);
                }
                case "250to299" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 250000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 299999.0);
                }
                case "150to179" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 150000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 179999.0);
                }
                case "180to209" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 180000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 209999.0);
                }
                case "210to239" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 210000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 239999.0);
                }
                case "240to269" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 240000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 269999.0);
                }
                case "270to299" -> {
                    resolvedPriceMin = resolveMin(resolvedPriceMin, 270000.0);
                    resolvedPriceMax = resolveMax(resolvedPriceMax, 299999.0);
                }
                case "over300" -> resolvedPriceMin = resolveMin(resolvedPriceMin, 300000.0);
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
        String normalizedKeyword = blankToNull(keyword);
        String normalizedSort = normalizeSort(sort, normalizedKeyword);
        ProductSearchService.ProductSearchResult searchResult = productSearchService != null
                ? productSearchService.searchProductIds(normalizedKeyword, 128)
                : ProductSearchService.ProductSearchResult.notHandled();
        List<Long> candidateIds = searchResult.handled() ? searchResult.orderedIds() : null;
        boolean useSearchRanking = searchResult.handled()
                && normalizedKeyword != null
                && "relevance".equals(normalizedSort);
        Sort requestedSort = switch (normalizedSort) {
            case "name_desc" -> Sort.by(Sort.Order.desc("name").ignoreCase());
            case "price_asc" -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            default -> Sort.by(Sort.Order.asc("name").ignoreCase());
        };
        List<Product> products;
        long totalElements;
        int totalPages;
        int safePage;
        int activeFilterCount = countActiveFilters(
                keyword, brand, storage, priceRange, priceMin, priceMax, batteryRange, batteryMin, batteryMax, screenSize);

        if (searchResult.handled() && candidateIds != null && candidateIds.isEmpty()) {
            products = List.of();
            totalElements = 0;
            totalPages = 1;
            safePage = 0;
        } else if (useSearchRanking) {
            List<Product> rankedProducts = resolveRankedSearchResults(
                    candidateIds,
                    resolvedPriceMin,
                    resolvedPriceMax,
                    blankToNull(brand),
                    blankToNull(storage),
                    blankToNull(batteryRange),
                    batteryMin,
                    batteryMax,
                    blankToNull(screenSize));
            totalElements = rankedProducts.size();
            totalPages = Math.max((int) Math.ceil(totalElements / (double) effectivePageSize), 1);
            safePage = Math.max(0, Math.min(safeRequestedPage, totalPages - 1));
            int fromIndex = Math.min(safePage * effectivePageSize, rankedProducts.size());
            int toIndex = Math.min(fromIndex + effectivePageSize, rankedProducts.size());
            products = rankedProducts.subList(fromIndex, toIndex);
        } else {
            Page<Product> productPage = productRepository.findAll(
                    ProductCatalogSpecifications.forCatalog(
                            searchResult.handled() ? null : normalizedKeyword,
                            candidateIds,
                            resolvedPriceMin,
                            resolvedPriceMax,
                            blankToNull(brand),
                            blankToNull(storage),
                            blankToNull(batteryRange),
                            batteryMin,
                            batteryMax,
                            blankToNull(screenSize)),
                    PageRequest.of(safeRequestedPage, effectivePageSize, requestedSort));
            if (productPage.isEmpty() && safeRequestedPage > 0 && productPage.getTotalPages() > 0) {
                productPage = productRepository.findAll(
                        ProductCatalogSpecifications.forCatalog(
                                searchResult.handled() ? null : normalizedKeyword,
                                candidateIds,
                                resolvedPriceMin,
                                resolvedPriceMax,
                                blankToNull(brand),
                                blankToNull(storage),
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
        }

        List<String> brands = resolveAvailableBrands();
        return new CatalogPageResponse(
                products.stream()
                        .map(product -> apiMapper.toProductSummary(product, false))
                        .toList(),
                safePage,
                totalPages,
                totalElements,
                effectivePageSize,
                brands,
                activeFilterCount,
                activeFilterCount > 0);
    }

    private ProductDetailResponse buildProductDetailPublicResponse(long id, Long requestedVariantId) {
        Product product = productRepository.findWithRefsById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found."));
        ProductVariant selectedVariant = productCommerceService.resolveVariantOrDefault(product, requestedVariantId);
        ProductSummary summary = apiMapper.toProductSummary(product, false);

        if (selectedVariant != null && summary != null) {
            double resolvedVariantPrice = productCommerceService.resolveEffectivePrice(product, selectedVariant);
            summary = new ProductSummary(
                    summary.id(),
                    summary.name(),
                    summary.brand(),
                    resolvedVariantPrice,
                    summary.imageUrl(),
                    selectedVariant.getStock(),
                    selectedVariant.getStock() != null && selectedVariant.getStock() > 0,
                    selectedVariant.getStock() != null && selectedVariant.getStock() > 0 && selectedVariant.getStock() <= 3,
                    selectedVariant.getStock() == null || selectedVariant.getStock() <= 0
                            ? "Out of stock"
                            : (selectedVariant.getStock() <= 3
                                    ? "Only " + selectedVariant.getStock() + " left"
                                    : "In stock"),
                    resolvedVariantPrice > 0
                            ? Math.round(resolvedVariantPrice / 24.0)
                            : 0L,
                    selectedVariant.getStorage() != null ? selectedVariant.getStorage() : summary.storage(),
                    selectedVariant.getRam() != null ? selectedVariant.getRam() : summary.ram(),
                    summary.size(),
                    summary.os(),
                    summary.chipset(),
                    summary.speed(),
                    summary.resolution(),
                    summary.battery(),
                    summary.charging(),
                    summary.description(),
                    summary.wishlisted(),
                    summary.categoryId(),
                    summary.categoryName(),
                    summary.slug(),
                    summary.skuPrefix(),
                    selectedVariant.getId(),
                    selectedVariant.label());
        }

        List<ProductSummary> recommendedProducts = resolveRecommendedProducts(product).stream()
                .map(recommended -> apiMapper.toProductSummary(recommended, false))
                .toList();

        List<ProductVariantResponse> variants = productCommerceService.loadVariants(product.getId()).stream()
                .map(variant -> apiMapper.toProductVariantResponse(product, variant,
                        selectedVariant != null ? selectedVariant.getId() : null))
                .toList();

        List<ProductImageResponse> images = productCommerceService.loadImages(product.getId()).stream()
                .map(apiMapper::toProductImageResponse)
                .toList();

        List<ProductSpecResponse> specs = productCommerceService.loadSpecs(product.getId()).stream()
                .map(apiMapper::toProductSpecResponse)
                .toList();

        return new ProductDetailResponse(
                summary,
                recommendedProducts,
                false,
                variants,
                images,
                specs,
                selectedVariant != null ? selectedVariant.getId() : null);
    }

    private List<Product> resolveRecommendedProducts(Product currentProduct) {
        if (currentProduct == null || currentProduct.getId() == null) {
            return List.of();
        }
        List<Long> coPurchaseIds = productRepository.findRecommendedProductIdsByCoPurchase(
                currentProduct.getId(),
                PageRequest.of(0, 4));
        if (!coPurchaseIds.isEmpty()) {
            return orderProductsByIds(coPurchaseIds);
        }
        double targetPrice = currentProduct.getEffectivePrice() == null ? 0.0 : currentProduct.getEffectivePrice();
        return productRepository.findRecommendedProducts(
                currentProduct.getId(),
                targetPrice,
                PageRequest.of(0, 4));
    }

    private List<Product> resolveRankedSearchResults(
            List<Long> candidateIds,
            Double priceMin,
            Double priceMax,
            String brand,
            String storage,
            String batteryRange,
            Integer batteryMin,
            Integer batteryMax,
            String screenSize) {
        if (candidateIds == null || candidateIds.isEmpty()) {
            return List.of();
        }
        List<Product> filtered = productRepository.findAll(ProductCatalogSpecifications.forCatalog(
                null,
                candidateIds,
                priceMin,
                priceMax,
                brand,
                storage,
                batteryRange,
                batteryMin,
                batteryMax,
                screenSize));
        Map<Long, Integer> rankById = new LinkedHashMap<>();
        for (int index = 0; index < candidateIds.size(); index++) {
            rankById.put(candidateIds.get(index), index);
        }
        return filtered.stream()
                .sorted((left, right) -> Integer.compare(
                        rankById.getOrDefault(left.getId(), Integer.MAX_VALUE),
                        rankById.getOrDefault(right.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    private List<Product> orderProductsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<Product> products = productRepository.findAllByIdIn(ids);
        Map<Long, Product> byId = new LinkedHashMap<>();
        for (Product product : products) {
            if (product != null && product.getId() != null) {
                byId.put(product.getId(), product);
            }
        }
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    private List<String> resolveAvailableBrands() {
        return getOrLoadCache(BRAND_LIST_CACHE, "all", this::loadAvailableBrands);
    }

    private List<String> loadAvailableBrands() {
        List<String> names = productRepository.findAllBrandNamesOrdered();
        if (names != null && !names.isEmpty()) {
            return names;
        }
        return productRepository.findAllNamesOrdered().stream()
                .map(this::inferBrandNameFallback)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();
    }

    private String inferBrandNameFallback(String name) {
        String slug = productCommerceService.inferBrandSlug(name);
        return switch (slug) {
            case "apple" -> "Apple";
            case "samsung" -> "Samsung";
            case "google" -> "Google";
            case "oppo" -> "OPPO";
            case "vivo" -> "Vivo";
            case "xiaomi" -> "Xiaomi";
            case "sony" -> "Sony";
            case "asus" -> "ASUS";
            case "zte" -> "ZTE";
            case "huawei" -> "Huawei";
            case "honor" -> "Honor";
            default -> "Other";
        };
    }

    private Set<Long> resolveWishlistedProductIds(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken) {
            return Set.of();
        }
        return wishlistService.getWishlistedProductIds(authentication.getName());
    }

    private CatalogPageResponse applyWishlistToCatalog(CatalogPageResponse source, Set<Long> wishlistedProductIds) {
        List<ProductSummary> products = source.products().stream()
                .map(product -> applyWishlistFlag(product, wishlistedProductIds))
                .toList();
        return new CatalogPageResponse(
                products,
                source.currentPage(),
                source.totalPages(),
                source.totalElements(),
                source.pageSize(),
                source.brands(),
                source.activeFilterCount(),
                source.hasActiveFilters());
    }

    private ProductDetailResponse applyWishlistToProductDetail(ProductDetailResponse source, Set<Long> wishlistedProductIds) {
        ProductSummary product = applyWishlistFlag(source.product(), wishlistedProductIds);
        List<ProductSummary> recommendedProducts = source.recommendedProducts().stream()
                .map(item -> applyWishlistFlag(item, wishlistedProductIds))
                .toList();
        boolean wishlisted = product != null && product.id() != null && wishlistedProductIds.contains(product.id());
        return new ProductDetailResponse(
                product,
                recommendedProducts,
                wishlisted,
                source.variants(),
                source.images(),
                source.specs(),
                source.selectedVariantId());
    }

    private CatalogPageResponse normalizeCatalogPageResponse(CatalogPageResponse source) {
        if (source == null) {
            return new CatalogPageResponse(List.of(), 0, 1, 0, DESKTOP_PAGE_SIZE, List.of(), 0, false);
        }
        return new CatalogPageResponse(
                source.products() == null ? List.of() : source.products(),
                source.currentPage(),
                source.totalPages(),
                source.totalElements(),
                source.pageSize(),
                source.brands() == null ? List.of() : source.brands(),
                source.activeFilterCount(),
                source.hasActiveFilters());
    }

    private ProductDetailResponse normalizeProductDetailResponse(ProductDetailResponse source) {
        if (source == null) {
            throw new NoSuchElementException("Product detail not found.");
        }
        return new ProductDetailResponse(
                source.product(),
                source.recommendedProducts() == null ? List.of() : source.recommendedProducts(),
                source.wishlisted(),
                source.variants() == null ? List.of() : source.variants(),
                source.images() == null ? List.of() : source.images(),
                source.specs() == null ? List.of() : source.specs(),
                source.selectedVariantId());
    }

    private ProductSummary applyWishlistFlag(ProductSummary product, Set<Long> wishlistedProductIds) {
        if (product == null || product.id() == null) {
            return product;
        }
        boolean wishlisted = wishlistedProductIds.contains(product.id());
        if (wishlisted == product.wishlisted()) {
            return product;
        }
        return new ProductSummary(
                product.id(),
                product.name(),
                product.brand(),
                product.price(),
                product.imageUrl(),
                product.stock(),
                product.available(),
                product.lowStock(),
                product.availabilityLabel(),
                product.monthlyInstallmentAmount(),
                product.storage(),
                product.ram(),
                product.size(),
                product.os(),
                product.chipset(),
                product.speed(),
                product.resolution(),
                product.battery(),
                product.charging(),
                product.description(),
                wishlisted,
                product.categoryId(),
                product.categoryName(),
                product.slug(),
                product.skuPrefix(),
                product.defaultVariantId(),
                product.defaultVariantLabel());
    }

    private <T> T getOrLoadCache(@NonNull String cacheName, @NonNull String key, @NonNull Supplier<T> valueLoader) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return Objects.requireNonNull(valueLoader.get(), "Cache loader must not return null.");
        }
        try {
            T value = cache.get(key, valueLoader::get);
            return Objects.requireNonNull(value, "Cache value must not be null.");
        } catch (RuntimeException ex) {
            log.warn("Cache read failed for {}:{}; evicting poisoned entry and reloading.", cacheName, key, ex);
            cache.evict(key);
            T value = valueLoader.get();
            return Objects.requireNonNull(value, "Cache loader must not return null.");
        }
    }

    private int countActiveFilters(String keyword, String brand, String storage, String priceRange,
            Double priceMin, Double priceMax, String batteryRange, Integer batteryMin,
            Integer batteryMax, String screenSize) {
        int count = 0;
        if (keyword != null && !keyword.isBlank()) count++;
        if (brand != null && !brand.isBlank()) count++;
        if (storage != null && !storage.isBlank()) count++;
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

    private String normalizeSort(String sort, String keyword) {
        if (sort == null || sort.isBlank()) {
            return keyword == null ? "name_asc" : "relevance";
        }
        return switch (sort) {
            case "relevance", "name_desc", "price_asc", "price_desc" -> sort;
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
