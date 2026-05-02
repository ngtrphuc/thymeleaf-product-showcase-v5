package io.github.ngtrphuc.smartphone_shop.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.ProductImage;
import io.github.ngtrphuc.smartphone_shop.model.ProductSpec;
import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import io.github.ngtrphuc.smartphone_shop.repository.BrandRepository;
import io.github.ngtrphuc.smartphone_shop.repository.CategoryRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductImageRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductSpecRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductVariantRepository;

@Service
public class ProductCommerceService {

    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecRepository productSpecRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;

    public ProductCommerceService(@Nullable ProductVariantRepository productVariantRepository,
            @Nullable ProductImageRepository productImageRepository,
            @Nullable ProductSpecRepository productSpecRepository,
            @Nullable BrandRepository brandRepository,
            @Nullable CategoryRepository categoryRepository) {
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.productSpecRepository = productSpecRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<ProductVariant> loadVariants(Long productId) {
        if (productId == null || productVariantRepository == null) {
            return List.of();
        }
        return productVariantRepository.findByProductIdOrderByActiveDescIdAsc(productId);
    }

    public List<ProductImage> loadImages(Long productId) {
        if (productId == null || productImageRepository == null) {
            return List.of();
        }
        return productImageRepository.findByProductIdOrdered(productId);
    }

    public List<ProductSpec> loadSpecs(Long productId) {
        if (productId == null || productSpecRepository == null) {
            return List.of();
        }
        return productSpecRepository.findByProductIdOrdered(productId);
    }

    public ProductVariant resolveVariantOrDefault(Product product, Long requestedVariantId) {
        if (product == null || product.getId() == null) {
            return null;
        }
        if (productVariantRepository == null) {
            return null;
        }
        List<ProductVariant> variants = productVariantRepository.findActiveByProductId(product.getId());
        if (variants.isEmpty()) {
            return null;
        }
        if (requestedVariantId != null) {
            for (ProductVariant variant : variants) {
                if (Objects.equals(variant.getId(), requestedVariantId)) {
                    return variant;
                }
            }
        }
        return variants.stream()
                .sorted(Comparator.comparing(ProductVariant::getId, Comparator.nullsLast(Long::compareTo)))
                .findFirst()
                .orElse(null);
    }

    public String resolvePrimaryImageUrl(Product product, List<ProductImage> images) {
        if (images != null && !images.isEmpty()) {
            ProductImage primary = images.stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .orElse(images.stream()
                            .sorted(Comparator.comparing(ProductImage::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                                    .thenComparing(ProductImage::getId, Comparator.nullsLast(Long::compareTo)))
                            .findFirst()
                            .orElse(null));
            if (primary != null && primary.getUrl() != null && !primary.getUrl().isBlank()) {
                return primary.getUrl();
            }
        }
        return product != null ? product.getImageUrl() : null;
    }

    public int resolveEffectiveStock(Product product, ProductVariant variant) {
        if (variant != null) {
            return Math.max(0, Optional.ofNullable(variant.getStock()).orElse(0));
        }
        if (product == null || product.getId() == null) {
            return 0;
        }
        if (productVariantRepository == null) {
            return Math.max(0, Optional.ofNullable(product.getStock()).orElse(0));
        }
        List<ProductVariant> variants = productVariantRepository.findActiveByProductId(product.getId());
        if (!variants.isEmpty()) {
            return variants.stream()
                    .map(ProductVariant::getStock)
                    .filter(v -> v != null && v > 0)
                    .mapToInt(Integer::intValue)
                    .sum();
        }
        return Math.max(0, Optional.ofNullable(product.getStock()).orElse(0));
    }

    public double resolveEffectivePrice(Product product, ProductVariant variant) {
        if (variant != null && variant.getPriceOverride() != null && variant.getPriceOverride() >= 0) {
            return variant.getPriceOverride();
        }
        if (product == null) {
            return 0.0;
        }
        Double base = product.getBasePrice();
        if (base != null && base >= 0) {
            return base;
        }
        return Optional.ofNullable(product.getPrice()).orElse(0.0);
    }

    public String resolveVariantLabel(ProductVariant variant) {
        if (variant == null) {
            return null;
        }
        return variant.label();
    }

    public void ensureDefaultCategoryAndBrand(Product product) {
        if (product == null) {
            return;
        }
        if (product.getCategory() == null && categoryRepository != null) {
            categoryRepository.findBySlugIgnoreCase("smartphones").ifPresent(product::setCategory);
        }
        if (product.getBrand() == null && brandRepository != null) {
            String slug = inferBrandSlug(product.getName());
            brandRepository.findBySlugIgnoreCase(slug).ifPresent(product::setBrand);
        }
    }

    public String inferBrandSlug(String productName) {
        String value = productName == null ? "" : productName.trim().toLowerCase(Locale.ROOT);
        if (value.startsWith("apple iphone") || value.startsWith("iphone")) return "apple";
        if (value.startsWith("samsung") || value.startsWith("galaxy")) return "samsung";
        if (value.startsWith("google") || value.startsWith("pixel")) return "google";
        if (value.startsWith("oppo") || value.startsWith("find ")) return "oppo";
        if (value.startsWith("vivo")) return "vivo";
        if (value.startsWith("xiaomi")) return "xiaomi";
        if (value.startsWith("sony") || value.startsWith("xperia")) return "sony";
        if (value.startsWith("asus") || value.startsWith("rog")) return "asus";
        if (value.startsWith("zte") || value.startsWith("nubia") || value.startsWith("redmagic")) return "zte";
        if (value.startsWith("huawei")) return "huawei";
        if (value.startsWith("honor")) return "honor";
        return "other";
    }
}
