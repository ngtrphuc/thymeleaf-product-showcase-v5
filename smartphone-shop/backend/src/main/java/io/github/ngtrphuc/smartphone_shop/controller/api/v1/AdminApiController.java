package io.github.ngtrphuc.smartphone_shop.controller.api.v1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;

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

import io.github.ngtrphuc.smartphone_shop.api.ApiMapper;
import io.github.ngtrphuc.smartphone_shop.api.dto.ChatMessageResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OperationStatusResponse;
import io.github.ngtrphuc.smartphone_shop.api.dto.OrderResponse;
import io.github.ngtrphuc.smartphone_shop.common.support.StorefrontSupport;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.ChatService;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminApiController {

    private static final int DEFAULT_PAGE_SIZE = 10;

    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderService orderService;
    private final ChatService chatService;
    private final ApiMapper apiMapper;

    public AdminApiController(ProductRepository productRepository,
            CartItemRepository cartItemRepository,
            OrderService orderService,
            ChatService chatService,
            ApiMapper apiMapper) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderService = orderService;
        this.chatService = chatService;
        this.apiMapper = apiMapper;
    }

    @GetMapping("/dashboard")
    public DashboardResponse dashboard(@RequestParam(name = "page", defaultValue = "0") int page) {
        long totalOrders = orderService.countOrders();
        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / DEFAULT_PAGE_SIZE);
        int safePage = totalPages == 0 ? 0 : Math.max(0, Math.min(page, totalPages - 1));

        return new DashboardResponse(
                productRepository.count(),
                orderService.getTotalItemsSold(),
                totalOrders,
                orderService.getTotalRevenue(),
                safePage,
                totalPages,
                orderService.getRecentOrders(safePage, DEFAULT_PAGE_SIZE).stream().map(apiMapper::toOrderResponse).toList());
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

        List<Product> items;
        int currentPage;
        int totalPages;
        long totalElements;

        if (normalizedBrand == null) {
            Page<Product> result = productRepository.findAdminProducts(
                    normalizedKeyword,
                    keywordId,
                    minStock,
                    maxStock,
                    PageRequest.of(safePage, safeSize, requestedSort));
            items = result.getContent();
            totalElements = result.getTotalElements();
            totalPages = result.getTotalPages();
            currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(result.getNumber(), totalPages - 1));
        } else {
            List<Product> filtered = productRepository.findAllAdminProducts(
                    normalizedKeyword,
                    keywordId,
                    minStock,
                    maxStock).stream()
                    .filter(product -> normalizedBrand.equalsIgnoreCase(StorefrontSupport.extractBrand(
                            product != null ? product.getName() : null)))
                    .toList();

            List<Product> sorted = applySort(filtered, normalizedSort);
            totalElements = sorted.size();
            totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
            currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(safePage, totalPages - 1));

            int from = Math.min(currentPage * safeSize, sorted.size());
            int to = Math.min(from + safeSize, sorted.size());
            items = sorted.subList(from, to);
        }

        List<String> brands = productRepository.findAllNamesOrdered().stream()
                .map(StorefrontSupport::extractBrand)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        return new AdminProductPageResponse(items, currentPage, totalPages, totalElements, safeSize, brands);
    }

    @PostMapping("/products")
    @CacheEvict(value = { "catalogPublic", "productDetailPublic" }, allEntries = true)
    public Product createProduct(@RequestBody Product request) {
        Product product = new Product();
        applyProductInput(product, request);
        return Objects.requireNonNull(productRepository.save(product));
    }

    @PutMapping("/products/{id}")
    @CacheEvict(value = { "catalogPublic", "productDetailPublic" }, allEntries = true)
    public Product updateProduct(@PathVariable(name = "id") long id, @RequestBody Product request) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found."));
        applyProductInput(existing, request);
        return Objects.requireNonNull(productRepository.save(existing));
    }

    @DeleteMapping("/products/{id}")
    @Transactional
    @CacheEvict(value = { "catalogPublic", "productDetailPublic" }, allEntries = true)
    public OperationStatusResponse deleteProduct(@PathVariable(name = "id") long id) {
        if (!productRepository.existsById(id)) {
            throw new NoSuchElementException("Product not found.");
        }
        cartItemRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        return new OperationStatusResponse(true, "Product deleted successfully.");
    }

    @GetMapping("/orders")
    public AdminOrderPageResponse orders(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "pageSize", defaultValue = "10") int pageSize) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(pageSize, 50));

        long totalOrders = orderService.countOrders();
        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / safeSize);
        int currentPage = totalPages == 0 ? 0 : Math.max(0, Math.min(safePage, totalPages - 1));

        List<OrderResponse> orders = orderService.getAdminOrdersPage(currentPage, safeSize)
                .stream()
                .map(apiMapper::toOrderResponse)
                .toList();
        return new AdminOrderPageResponse(orders, currentPage, totalPages, totalOrders, safeSize);
    }

    @PostMapping("/orders/{id}/status")
    public OperationStatusResponse updateOrderStatus(@PathVariable(name = "id") long id,
            @RequestBody UpdateOrderStatusRequest request) {
        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new IllegalArgumentException("Order status is required.");
        }
        orderService.updateStatus(id, request.status());
        return new OperationStatusResponse(true, "Order status updated.");
    }

    @GetMapping("/chat/conversations")
    public AdminConversationsResponse conversations() {
        return new AdminConversationsResponse(
                chatService.getAllConversationEmails(),
                new LinkedHashMap<>(chatService.getUnreadCountsByAdminConversation()));
    }

    @GetMapping("/chat/history")
    public List<ChatMessageResponse> chatHistory(@RequestParam(name = "email") String email) {
        return chatService.getHistory(email)
                .stream()
                .map(apiMapper::toChatMessageResponse)
                .toList();
    }

    @PostMapping("/chat/messages")
    public ChatMessageResponse sendAdminMessage(@RequestBody AdminSendMessageRequest request) {
        if (request == null || request.userEmail() == null || request.userEmail().isBlank()
                || request.content() == null || request.content().isBlank()) {
            throw new IllegalArgumentException("Conversation email and message content are required.");
        }
        return apiMapper.toChatMessageResponse(
                chatService.saveAdminMessage(request.userEmail(), request.content()));
    }

    @PostMapping("/chat/read")
    public OperationStatusResponse markConversationRead(@RequestBody MarkReadRequest request) {
        if (request == null || request.userEmail() == null || request.userEmail().isBlank()) {
            throw new IllegalArgumentException("Conversation email is required.");
        }
        chatService.markReadByAdmin(request.userEmail());
        return new OperationStatusResponse(true, "Conversation marked as read.");
    }

    @GetMapping("/chat/unread-count")
    public long unreadCount() {
        return chatService.countAllUnreadByAdmin();
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

    private List<Product> applySort(List<Product> products, String sort) {
        List<Product> sorted = new ArrayList<>(products);
        switch (sort) {
            case "id_desc" -> sorted.sort((left, right) -> Long.compare(safeId(right), safeId(left)));
            case "name_asc" -> sorted.sort((left, right) -> safeName(left).compareTo(safeName(right)));
            case "name_desc" -> sorted.sort((left, right) -> safeName(right).compareTo(safeName(left)));
            case "price_asc" -> sorted.sort((left, right) -> Double.compare(safePrice(left), safePrice(right)));
            case "price_desc" -> sorted.sort((left, right) -> Double.compare(safePrice(right), safePrice(left)));
            case "stock_asc" -> sorted.sort((left, right) -> Integer.compare(safeStock(left), safeStock(right)));
            case "stock_desc" -> sorted.sort((left, right) -> Integer.compare(safeStock(right), safeStock(left)));
            default -> sorted.sort((left, right) -> safeName(left).compareTo(safeName(right)));
        }
        return sorted;
    }

    private long safeId(Product product) {
        Long id = product != null ? product.getId() : null;
        return id != null ? id : Long.MAX_VALUE;
    }

    private String safeName(Product product) {
        String name = product != null ? product.getName() : null;
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private double safePrice(Product product) {
        Double price = product != null ? product.getPrice() : null;
        return price != null ? price : Double.MAX_VALUE;
    }

    private int safeStock(Product product) {
        Integer stock = product != null ? product.getStock() : null;
        return stock != null ? stock : Integer.MAX_VALUE;
    }

    public record DashboardResponse(
            long totalProducts,
            long totalItemsSold,
            long totalOrders,
            double totalRevenue,
            int currentPage,
            int totalPages,
            List<OrderResponse> recentOrders) {
    }

    public record AdminProductPageResponse(
            List<Product> products,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize,
            List<String> brands) {
    }

    public record AdminOrderPageResponse(
            List<OrderResponse> orders,
            int currentPage,
            int totalPages,
            long totalElements,
            int pageSize) {
    }

    public record UpdateOrderStatusRequest(String status) {
    }

    public record AdminConversationsResponse(List<String> emails, Map<String, Long> unreadCounts) {
    }

    public record AdminSendMessageRequest(String userEmail, String content) {
    }

    public record MarkReadRequest(String userEmail) {
    }
}
