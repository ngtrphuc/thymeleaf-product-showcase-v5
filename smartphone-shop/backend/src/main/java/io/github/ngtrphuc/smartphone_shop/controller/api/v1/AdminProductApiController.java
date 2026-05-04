package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.github.ngtrphuc.smartphone_shop.api.dto.OperationStatusResponse;
import io.github.ngtrphuc.smartphone_shop.model.Brand;
import io.github.ngtrphuc.smartphone_shop.model.Category;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.ProductImage;
import io.github.ngtrphuc.smartphone_shop.model.ProductSpec;
import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import io.github.ngtrphuc.smartphone_shop.repository.BrandRepository;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.CategoryRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductImageRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductSpecRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductVariantRepository;
import io.github.ngtrphuc.smartphone_shop.service.ProductCommerceService;
import io.github.ngtrphuc.smartphone_shop.service.ProductSearchService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProductApiController {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecRepository productSpecRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ProductCommerceService productCommerceService;
    private ProductSearchService productSearchService;

    public AdminProductApiController(ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            ProductSpecRepository productSpecRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            ProductCommerceService productCommerceService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.productSpecRepository = productSpecRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.productCommerceService = productCommerceService;
    }

    @Autowired(required = false)
    void setProductSearchService(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping("/products")
    public AdminProductPageResponse products(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "stock", defaultValue = "all") String stock,
            @RequestParam(name = "sort", defaultValue = "name_asc") String sort) {

        String normalizedKeyword = normalizeText(keyword);
        String normalizedBrand = normalizeText(brand);
        String normalizedStock = normalizeStock(stock);
        String normalizedSort = normalizeSort(sort);

        Long keywordId = parseKeywordId(normalizedKeyword);
        Integer minStock = resolveMinStock(normalizedStock);
        Integer maxStock = resolveMaxStock(normalizedStock);

        int safeSize = Math.max(1, Math.min(pageSize, 50));
        int safePage = Math.max(page, 0);
        Sort requestedSort = Objects.requireNonNull(resolveAdminSort(normalizedSort));

        Page<Product> result = productRepository.findAdminProducts(
                normalizedKeyword,
                keywordId,
                minStock,
                maxStock,
                normalizedBrand,
                PageRequest.of(safePage, safeSize, requestedSort));

        List<AdminProductResponse> items = result.getContent().stream()
                .map(this::toAdminProductResponse)
                .toList();
        int totalPages = result.getTotalPages();
        int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(result.getNumber(), totalPages - 1));

        List<String> brands = productRepository.findAllBrandNamesOrdered();
        if (brands == null || brands.isEmpty()) {
            brands = productRepository.findAllNamesOrdered().stream()
                    .map(productCommerceService::inferBrandSlug)
                    .map(this::displayBrandName)
                    .distinct()
                    .sorted(String.CASE_INSENSITIVE_ORDER)
                    .toList();
        }

        return new AdminProductPageResponse(
                items,
                currentPage,
                totalPages,
                result.getTotalElements(),
                safeSize,
                brands);
    }

    @PostMapping("/products")
    @Transactional
    @CacheEvict(value = { "catalogPublic", "productDetailPublic", "brandList" }, allEntries = true)
    public AdminProductResponse createProduct(@RequestBody AdminProductUpsertRequest request) {
        Product product = new Product();
        applyProductInput(product, request);
        Product saved = productRepository.save(product);

        replaceProductDetails(saved, request);
        Product updated = productRepository.save(saved);

        if (productSearchService != null) {
            productSearchService.syncProduct(updated);
        }
        return toAdminProductResponse(updated);
    }

    @PutMapping("/products/{id}")
    @Transactional
    @CacheEvict(value = { "catalogPublic", "productDetailPublic", "brandList" }, allEntries = true)
    public AdminProductResponse updateProduct(@PathVariable(name = "id") long id, @RequestBody AdminProductUpsertRequest request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found."));
        applyProductInput(existing, request);
        Product saved = productRepository.save(Objects.requireNonNull(existing));

        replaceProductDetails(saved, request);
        Product updated = productRepository.save(saved);

        if (productSearchService != null) {
            productSearchService.syncProduct(updated);
        }
        return toAdminProductResponse(updated);
    }

    private AdminProductResponse toAdminProductResponse(Product product) {
        if (product == null) {
            return null;
        }
        Double resolvedPrice = product.getBasePrice() != null ? product.getBasePrice() : product.getPrice();
        return new AdminProductResponse(
                product.getId(),
                product.getName(),
                resolvedPrice,
                product.getBasePrice(),
                product.getImageUrl(),
                product.getStock(),
                product.getOs(),
                product.getChipset(),
                product.getSpeed(),
                product.getRam(),
                product.getStorage(),
                product.getSize(),
                product.getResolution(),
                product.getBattery(),
                product.getCharging(),
                product.getDescription(),
                product.getSlug(),
                product.getSkuPrefix(),
                product.getActive(),
                null,
                null,
                null,
                null);
    }

    @DeleteMapping("/products/{id}")
    @Transactional
    @CacheEvict(value = { "catalogPublic", "productDetailPublic", "brandList" }, allEntries = true)
    public OperationStatusResponse deleteProduct(@PathVariable(name = "id") long id) {
        if (!productRepository.existsById(id)) {
            throw new NoSuchElementException("Product not found.");
        }
        cartItemRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        if (productSearchService != null) {
            productSearchService.deleteProduct(id);
        }
        return new OperationStatusResponse(true, "Product deleted successfully.");
    }

    private void applyProductInput(Product target, AdminProductUpsertRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Product payload is required.");
        }

        target.setName(normalizeRequiredField(request.name(), "Product name is required.", "Product name is too long.", 160));
        target.setSlug(normalizeOptionalField(request.slug(), "Slug is too long.", 180));
        target.setSkuPrefix(normalizeOptionalField(request.skuPrefix(), "SKU prefix is too long.", 40));
        target.setImageUrl(normalizeOptionalField(request.imageUrl(), "Image URL is too long.", 255));
        target.setOs(normalizeOptionalField(request.os(), "OS is too long.", 80));
        target.setRam(normalizeOptionalField(request.ram(), "RAM is too long.", 80));
        target.setChipset(normalizeOptionalField(request.chipset(), "Chipset is too long.", 120));
        target.setSpeed(normalizeOptionalField(request.speed(), "Speed is too long.", 80));
        target.setStorage(normalizeOptionalField(request.storage(), "Storage is too long.", 80));
        target.setSize(normalizeOptionalField(request.size(), "Screen size is too long.", 80));
        target.setResolution(normalizeOptionalField(request.resolution(), "Resolution is too long.", 120));
        target.setBattery(normalizeOptionalField(request.battery(), "Battery field is too long.", 80));
        target.setCharging(normalizeOptionalField(request.charging(), "Charging field is too long.", 80));
        target.setDescription(normalizeOptionalField(request.description(), "Description is too long.", 1000));
        target.setActive(request.active() == null ? Boolean.TRUE : request.active());

        Double price = request.basePrice() != null ? request.basePrice() : request.price();
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Price must be zero or greater.");
        }
        target.setBasePrice(price);
        target.setPrice(price);

        Integer stock = request.stock();
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Stock must be zero or greater.");
        }
        target.setStock(stock);

        if (target.getSlug() == null || target.getSlug().isBlank()) {
            target.setSlug(slugify(target.getName()));
        }
        if (target.getSkuPrefix() == null || target.getSkuPrefix().isBlank()) {
            target.setSkuPrefix(defaultSkuPrefix(target.getName()));
        }

        productCommerceService.ensureDefaultCategoryAndBrand(target);

        Long categoryId = request.categoryId();
        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new IllegalArgumentException("Category not found."));
            target.setCategory(category);
        } else if (request.categorySlug() != null && !request.categorySlug().isBlank()) {
            categoryRepository.findBySlugIgnoreCase(request.categorySlug())
                    .ifPresent(target::setCategory);
        }

        Long brandId = request.brandId();
        if (brandId != null) {
            Brand brand = brandRepository.findById(brandId)
                    .orElseThrow(() -> new IllegalArgumentException("Brand not found."));
            target.setBrand(brand);
        } else if (request.brandSlug() != null && !request.brandSlug().isBlank()) {
            brandRepository.findBySlugIgnoreCase(request.brandSlug())
                    .ifPresent(target::setBrand);
        }

        String imageUrl = target.getImageUrl();
        if (imageUrl != null && !(imageUrl.startsWith("/") || imageUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("Image URL must start with '/' or https://.");
        }
    }

    private void replaceProductDetails(Product product, AdminProductUpsertRequest request) {
        Long productId = Objects.requireNonNull(product.getId(), "Product must be persisted before replacing details.");
        productVariantRepository.deleteByProduct_Id(productId);
        productImageRepository.deleteByProduct_Id(productId);
        productSpecRepository.deleteByProduct_Id(productId);

        List<ProductVariantRequest> variants = request.variants() == null ? List.of() : request.variants();
        if (variants.isEmpty()) {
            ProductVariant fallback = new ProductVariant();
            fallback.setProduct(product);
            fallback.setSku(product.getSkuPrefix() + "-" + productId);
            fallback.setColor("Default");
            fallback.setStorage(product.getStorage());
            fallback.setRam(product.getRam());
            fallback.setPriceOverride(null);
            fallback.setStock(Math.max(0, request.stock() != null ? request.stock() : 0));
            fallback.setActive(true);
            productVariantRepository.save(fallback);
        } else {
            for (int index = 0; index < variants.size(); index++) {
                ProductVariantRequest input = variants.get(index);
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product);
                String sku = normalizeOptionalField(input.sku(), "Variant SKU is too long.", 80);
                if (sku == null || sku.isBlank()) {
                    sku = product.getSkuPrefix() + "-V" + (index + 1);
                }
                if (productVariantRepository.existsBySkuIgnoreCase(sku)) {
                    sku = sku + "-" + productId + "-" + (index + 1);
                }
                variant.setSku(sku);
                variant.setColor(normalizeOptionalField(input.color(), "Variant color is too long.", 80));
                variant.setStorage(normalizeOptionalField(input.storage(), "Variant storage is too long.", 80));
                variant.setRam(normalizeOptionalField(input.ram(), "Variant RAM is too long.", 80));
                variant.setPriceOverride(input.priceOverride() != null && input.priceOverride() >= 0 ? input.priceOverride() : null);
                variant.setStock(Math.max(0, input.stock() != null ? input.stock() : 0));
                variant.setActive(input.active() == null || input.active());
                productVariantRepository.save(variant);
            }
        }

        List<ProductImageRequest> images = request.images() == null ? List.of() : request.images();
        if (images.isEmpty()) {
            if (product.getImageUrl() != null && !product.getImageUrl().isBlank()) {
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setUrl(product.getImageUrl());
                image.setSortOrder(0);
                image.setPrimary(true);
                productImageRepository.save(image);
            }
        } else {
            for (int index = 0; index < images.size(); index++) {
                ProductImageRequest input = images.get(index);
                String url = normalizeRequiredField(input.url(), "Image URL is required.", "Image URL is too long.", 255);
                ProductImage image = new ProductImage();
                image.setProduct(product);
                image.setUrl(url);
                image.setSortOrder(input.sortOrder() != null ? input.sortOrder() : index);
                image.setPrimary(input.primary() != null ? input.primary() : index == 0);
                productImageRepository.save(image);
            }
        }

        List<ProductSpecRequest> specs = request.specs() == null ? List.of() : request.specs();
        if (specs.isEmpty()) {
            List<ProductSpec> fallbackSpecs = new ArrayList<>();
            addFallbackSpec(fallbackSpecs, product, "OS", product.getOs(), 10);
            addFallbackSpec(fallbackSpecs, product, "Chipset", product.getChipset(), 20);
            addFallbackSpec(fallbackSpecs, product, "CPU Speed", product.getSpeed(), 30);
            addFallbackSpec(fallbackSpecs, product, "RAM", product.getRam(), 40);
            addFallbackSpec(fallbackSpecs, product, "Storage", product.getStorage(), 50);
            addFallbackSpec(fallbackSpecs, product, "Screen Size", product.getSize(), 60);
            addFallbackSpec(fallbackSpecs, product, "Resolution", product.getResolution(), 70);
            addFallbackSpec(fallbackSpecs, product, "Battery", product.getBattery(), 80);
            addFallbackSpec(fallbackSpecs, product, "Charging", product.getCharging(), 90);
            if (!fallbackSpecs.isEmpty()) {
                productSpecRepository.saveAll(fallbackSpecs);
            }
        } else {
            for (int index = 0; index < specs.size(); index++) {
                ProductSpecRequest input = specs.get(index);
                ProductSpec spec = new ProductSpec();
                spec.setProduct(product);
                spec.setSpecKey(normalizeRequiredField(input.key(), "Spec key is required.", "Spec key is too long.", 120));
                spec.setSpecValue(normalizeOptionalField(input.value(), "Spec value is too long.", 255));
                spec.setSortOrder(input.sortOrder() != null ? input.sortOrder() : index);
                productSpecRepository.save(spec);
            }
        }

        List<ProductVariant> persistedVariants = productVariantRepository.findByProductIdOrderByActiveDescIdAsc(productId);
        int totalStock = persistedVariants.stream()
                .filter(ProductVariant::isActive)
                .map(ProductVariant::getStock)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .sum();
        ProductVariant first = persistedVariants.stream().filter(ProductVariant::isActive).findFirst().orElse(null);

        product.setStock(totalStock);
        if (first != null && first.effectivePrice() != null) {
            product.setPrice(first.effectivePrice());
            if (product.getBasePrice() == null || product.getBasePrice() < 0) {
                product.setBasePrice(first.effectivePrice());
            }
            if ((product.getStorage() == null || product.getStorage().isBlank()) && first.getStorage() != null) {
                product.setStorage(first.getStorage());
            }
            if ((product.getRam() == null || product.getRam().isBlank()) && first.getRam() != null) {
                product.setRam(first.getRam());
            }
        }

        List<ProductImage> persistedImages = productImageRepository.findByProductIdOrdered(productId);
        if (!persistedImages.isEmpty()) {
            product.setImageUrl(persistedImages.get(0).getUrl());
        }
    }

    private void addFallbackSpec(List<ProductSpec> specs,
            Product product,
            String key,
            String value,
            int sortOrder) {
        if (value == null || value.isBlank()) {
            return;
        }
        ProductSpec spec = new ProductSpec();
        spec.setProduct(product);
        spec.setSpecKey(key);
        spec.setSpecValue(value);
        spec.setSortOrder(sortOrder);
        specs.add(spec);
    }

    private String defaultSkuPrefix(String productName) {
        String normalized = productName == null ? "SKU" : productName.replaceAll("[^A-Za-z0-9]", "").toUpperCase(Locale.ROOT);
        if (normalized.length() >= 6) {
            return normalized.substring(0, 6);
        }
        return normalized.isBlank() ? "SKU" : normalized;
    }

    private String slugify(String value) {
        String normalized = value == null ? "product" : value.trim().toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        return normalized.isBlank() ? "product" : normalized;
    }

    private String normalizeRequiredField(String value, String emptyMessage, String tooLongMessage, int maxLength) {
        String normalized = normalizeOptionalField(value, tooLongMessage, maxLength);
        if (normalized == null) {
            throw new IllegalArgumentException(emptyMessage);
        }
        return normalized;
    }

    private String normalizeOptionalField(String value, String tooLongMessage, int maxLength) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            return null;
        }
        if (normalized.length() > maxLength) {
            throw new IllegalArgumentException(tooLongMessage);
        }
        return normalized;
    }

    private Long parseKeywordId(String keyword) {
        if (keyword == null || !keyword.chars().allMatch(Character::isDigit)) {
            return null;
        }
        try {
            return Long.valueOf(keyword);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeStock(String stock) {
        if (stock == null) {
            return "all";
        }
        return switch (stock) {
            case "in_stock", "low_stock", "out_of_stock" -> stock;
            default -> "all";
        };
    }

    private String normalizeSort(String sort) {
        if (sort == null) {
            return "name_asc";
        }
        return switch (sort) {
            case "id_desc", "name_asc", "name_desc", "price_asc", "price_desc", "stock_asc", "stock_desc" -> sort;
            default -> "name_asc";
        };
    }

    private Integer resolveMinStock(String stock) {
        return switch (stock) {
            case "in_stock", "low_stock" -> 1;
            default -> null;
        };
    }

    private Integer resolveMaxStock(String stock) {
        return switch (stock) {
            case "low_stock" -> 5;
            case "out_of_stock" -> 0;
            default -> null;
        };
    }

    private @NonNull Sort resolveAdminSort(String sort) {
        return switch (sort) {
            case "id_desc" -> Sort.by("id").descending();
            case "name_asc" -> Sort.by(Sort.Order.asc("name").ignoreCase());
            case "name_desc" -> Sort.by(Sort.Order.desc("name").ignoreCase());
            case "price_asc" -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            case "stock_asc" -> Sort.by("stock").ascending().and(Sort.by("id").ascending());
            case "stock_desc" -> Sort.by("stock").descending().and(Sort.by("id").ascending());
            default -> Sort.by(Sort.Order.asc("name").ignoreCase());
        };
    }

    private String displayBrandName(String slug) {
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

    public record AdminProductPageResponse(
            List<AdminProductResponse> products,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            List<String> brands) {
    }

    public record AdminProductResponse(
            Long id,
            String name,
            Double price,
            Double basePrice,
            String imageUrl,
            Integer stock,
            String os,
            String chipset,
            String speed,
            String ram,
            String storage,
            String size,
            String resolution,
            String battery,
            String charging,
            String description,
            String slug,
            String skuPrefix,
            Boolean active,
            Long brandId,
            String brandSlug,
            Long categoryId,
            String categorySlug) {
    }

    public record AdminProductUpsertRequest(
            String name,
            Double price,
            Double basePrice,
            String imageUrl,
            Integer stock,
            String os,
            String chipset,
            String speed,
            String ram,
            String storage,
            String size,
            String resolution,
            String battery,
            String charging,
            String description,
            String slug,
            String skuPrefix,
            Boolean active,
            Long brandId,
            String brandSlug,
            Long categoryId,
            String categorySlug,
            List<ProductVariantRequest> variants,
            List<ProductImageRequest> images,
            List<ProductSpecRequest> specs) {
    }

    public record ProductVariantRequest(
            String sku,
            String color,
            String storage,
            String ram,
            Double priceOverride,
            Integer stock,
            Boolean active) {
    }

    public record ProductImageRequest(
            String url,
            Integer sortOrder,
            Boolean primary) {
    }

    public record ProductSpecRequest(
            String key,
            String value,
            Integer sortOrder) {
    }
}
