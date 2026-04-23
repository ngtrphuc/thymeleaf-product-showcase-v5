package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.List;
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

import io.github.ngtrphuc.smartphone_shop.common.support.StorefrontSupport;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.api.dto.OperationStatusResponse;
import io.github.ngtrphuc.smartphone_shop.service.ProductSearchService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminProductApiController {

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private ProductSearchService productSearchService;

    public AdminProductApiController(ProductRepository productRepository,
            CartItemRepository cartItemRepository) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
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

        List<Product> items = result.getContent();
        int totalPages = result.getTotalPages();
        int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(result.getNumber(), totalPages - 1));

        List<String> brands = productRepository.findAllNamesOrdered().stream()
                .map(StorefrontSupport::extractBrand)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new AdminProductPageResponse(
                items,
                currentPage,
                totalPages,
                result.getTotalElements(),
                safeSize,
                brands);
    }

    @PostMapping("/products")
    @CacheEvict(value = { "catalogPublic", "productDetailPublic", "brandList" }, allEntries = true)
    public Product createProduct(@RequestBody Product request) {
        Product product = new Product();
        applyProductInput(product, request);
        Product saved = productRepository.save(product);
        if (productSearchService != null) {
            productSearchService.syncProduct(saved);
        }
        return saved;
    }

    @PutMapping("/products/{id}")
    @CacheEvict(value = { "catalogPublic", "productDetailPublic", "brandList" }, allEntries = true)
    public Product updateProduct(@PathVariable(name = "id") long id, @RequestBody Product request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found."));
        applyProductInput(existing, request);
        Product saved = productRepository.save(Objects.requireNonNull(existing));
        if (productSearchService != null) {
            productSearchService.syncProduct(saved);
        }
        return saved;
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

    private void applyProductInput(Product target, Product request) {
        if (request == null) {
            throw new IllegalArgumentException("Product payload is required.");
        }

        target.setName(normalizeRequiredField(request.getName(), "Product name is required.", "Product name is too long.", 160));
        target.setImageUrl(normalizeOptionalField(request.getImageUrl(), "Image URL is too long.", 255));
        target.setOs(normalizeOptionalField(request.getOs(), "OS is too long.", 80));
        target.setRam(normalizeOptionalField(request.getRam(), "RAM is too long.", 80));
        target.setChipset(normalizeOptionalField(request.getChipset(), "Chipset is too long.", 120));
        target.setSpeed(normalizeOptionalField(request.getSpeed(), "Speed is too long.", 80));
        target.setStorage(normalizeOptionalField(request.getStorage(), "Storage is too long.", 80));
        target.setSize(normalizeOptionalField(request.getSize(), "Screen size is too long.", 80));
        target.setResolution(normalizeOptionalField(request.getResolution(), "Resolution is too long.", 120));
        target.setBattery(normalizeOptionalField(request.getBattery(), "Battery field is too long.", 80));
        target.setCharging(normalizeOptionalField(request.getCharging(), "Charging field is too long.", 80));
        target.setDescription(normalizeOptionalField(request.getDescription(), "Description is too long.", 1000));

        Double price = request.getPrice();
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Price must be zero or greater.");
        }
        target.setPrice(price);

        Integer stock = request.getStock();
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Stock must be zero or greater.");
        }
        target.setStock(stock);

        String imageUrl = target.getImageUrl();
        if (imageUrl != null && !(imageUrl.startsWith("/") || imageUrl.startsWith("https://"))) {
            throw new IllegalArgumentException("Image URL must start with '/' or https://.");
        }
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
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "stock_asc" -> Sort.by("stock").ascending().and(Sort.by("id").ascending());
            case "stock_desc" -> Sort.by("stock").descending().and(Sort.by("id").ascending());
            default -> Sort.by(Sort.Order.asc("name").ignoreCase());
        };
    }

    public record AdminProductPageResponse(
            List<Product> products,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            List<String> brands) {
    }
}
