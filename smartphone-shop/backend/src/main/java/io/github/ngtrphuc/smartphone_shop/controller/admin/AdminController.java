package io.github.ngtrphuc.smartphone_shop.controller.admin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.service.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.service.OrderService;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final int ADMIN_PAGE_SIZE = 10;
    private static final int DASHBOARD_ORDER_PAGE_SIZE = 10;
    private static final int ADMIN_ORDER_PAGE_SIZE = 10;
    private final ProductRepository productRepository;
    private final CartItemRepository cartItemRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public AdminController(ProductRepository productRepository, CartItemRepository cartItemRepository,
            OrderRepository orderRepository, OrderService orderService) {
        this.productRepository = productRepository;
        this.cartItemRepository = cartItemRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    @GetMapping
    public String dashboard(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        long totalOrders = orderRepository.count();
        model.addAttribute("totalProducts", productRepository.count());
        model.addAttribute("totalItemsSold", orderService.getTotalItemsSold());
        model.addAttribute("totalOrders", totalOrders);
        model.addAttribute("totalRevenue", orderService.getTotalRevenue());

        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / DASHBOARD_ORDER_PAGE_SIZE);
        int safePage = totalPages == 0 ? 0 : Math.max(0, Math.min(page, totalPages - 1));

        model.addAttribute("recentOrders", orderService.getRecentOrders(safePage, DASHBOARD_ORDER_PAGE_SIZE));
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("dashboardOrderCount", totalOrders);
        model.addAttribute("dashboardPageSize", DASHBOARD_ORDER_PAGE_SIZE);
        return "dashboard";
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "stock", defaultValue = "all") String stock,
            @RequestParam(name = "sort", defaultValue = "name_asc") String sort,
            Model model) {
        populateProductListModel(model, page, keyword, brand, stock, sort);
        return "products";
    }

    @GetMapping("/products/new")
    public String newProductForm(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "stock", defaultValue = "all") String stock,
            @RequestParam(name = "sort", defaultValue = "name_asc") String sort,
            Model model) {
        if (!model.containsAttribute("product")) {
            model.addAttribute("product", new Product());
        }
        if (!model.containsAttribute("cancelUrl")) {
            model.addAttribute("cancelUrl", buildAdminProductsUrl(page, keyword, brand, stock, sort));
        }
        addProductListState(model, page, keyword, brand, stock, sort);
        return "product-form";
    }

    @GetMapping("/products/edit/{id}")
    public String editProductForm(@PathVariable(name = "id") long id,
            @RequestParam(name = "returnUrl", required = false) String returnUrl,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "stock", defaultValue = "all") String stock,
            @RequestParam(name = "sort", defaultValue = "name_asc") String sort,
            Model model,
            RedirectAttributes redirectAttributes) {
        return productRepository.findById(id)
                .map(product -> {
                    if (!model.containsAttribute("product")) {
                        model.addAttribute("product", product);
                    }
                    String normalizedReturnUrl = normalizeAdminReturnUrl(returnUrl);
                    if (!model.containsAttribute("returnUrl")) {
                        model.addAttribute("returnUrl", normalizedReturnUrl);
                    }
                    if (!model.containsAttribute("cancelUrl")) {
                        model.addAttribute("cancelUrl",
                                normalizedReturnUrl != null ? normalizedReturnUrl : buildAdminProductsUrl(page, keyword, brand, stock, sort));
                    }
                    addProductListState(model, page, keyword, brand, stock, sort);
                    return "product-form";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("toast", "Product not found.");
                    addProductListState(redirectAttributes, page, keyword, brand, stock, sort);
                    return "redirect:/admin/products";
                });
    }

    @PostMapping("/products/save")
    public String saveProduct(@ModelAttribute Product product,
            @RequestParam(name = "returnUrl", required = false) String returnUrl,
            @RequestParam(name = "returnPage", defaultValue = "0") int page,
            @RequestParam(name = "returnKeyword", required = false) String keyword,
            @RequestParam(name = "returnBrand", required = false) String brand,
            @RequestParam(name = "returnStock", defaultValue = "all") String stock,
            @RequestParam(name = "returnSort", defaultValue = "name_asc") String sort,
            RedirectAttributes redirectAttributes) {
        String normalizedReturnUrl = normalizeAdminReturnUrl(returnUrl);
        try {
            normalizeAndValidateProduct(product);
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
            redirectAttributes.addFlashAttribute("product", product);
            redirectAttributes.addFlashAttribute("returnUrl", normalizedReturnUrl);
            redirectAttributes.addFlashAttribute("cancelUrl",
                    normalizedReturnUrl != null ? normalizedReturnUrl : buildAdminProductsUrl(page, keyword, brand, stock, sort));
            addProductListState(redirectAttributes, page, keyword, brand, stock, sort);
            return product.getId() != null
                    ? "redirect:/admin/products/edit/" + product.getId()
                    : "redirect:/admin/products/new";
        }

        Long productId = product.getId();
        if (productId != null && !productRepository.existsById(productId)) {
            redirectAttributes.addFlashAttribute("toast", "Product not found.");
            addProductListState(redirectAttributes, page, keyword, brand, stock, sort);
            return "redirect:/admin/products";
        }

        boolean isUpdate = product.getId() != null;
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("toast",
                isUpdate ? "Product updated successfully." : "Product created successfully.");
        if (normalizedReturnUrl != null) {
            return "redirect:" + normalizedReturnUrl;
        }
        addProductListState(redirectAttributes, page, keyword, brand, stock, sort);
        return "redirect:/admin/products";
    }

    @PostMapping("/products/delete/{id}")
    @Transactional
    public String deleteProduct(@PathVariable(name = "id") long id,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "brand", required = false) String brand,
            @RequestParam(name = "stock", defaultValue = "all") String stock,
            @RequestParam(name = "sort", defaultValue = "name_asc") String sort,
            RedirectAttributes redirectAttributes) {
        if (!productRepository.existsById(id)) {
            redirectAttributes.addFlashAttribute("toast", "Product not found.");
            addProductListState(redirectAttributes, page, keyword, brand, stock, sort);
            return "redirect:/admin/products";
        }
        cartItemRepository.deleteByProductId(id);
        productRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("toast", "Product deleted successfully.");
        addProductListState(redirectAttributes, page, keyword, brand, stock, sort);
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String orders(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        long totalOrders = orderService.countOrders();
        int totalPages = totalOrders == 0 ? 0 : (int) Math.ceil((double) totalOrders / ADMIN_ORDER_PAGE_SIZE);
        int safePage = totalPages == 0 ? 0 : Math.max(0, Math.min(page, totalPages - 1));

        model.addAttribute("orders", orderService.getAdminOrdersPage(safePage, ADMIN_ORDER_PAGE_SIZE));
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("orderCount", totalOrders);
        return "orders";
    }

    @GetMapping("/access-denied-admin")
    public String accessDeniedAdminPage() {
        return "error/access-denied-admin";
    }

    @PostMapping("/orders/{id}/status")
    public String updateOrderStatus(@PathVariable(name = "id") long id,
            @RequestParam(name = "status") String status,
            @RequestParam(name = "page", defaultValue = "0") int page,
            RedirectAttributes redirectAttributes) {
        try {
            orderService.updateStatus(id, status);
            redirectAttributes.addFlashAttribute("toast", "Order status updated.");
        } catch (OrderValidationException ex) {
            redirectAttributes.addFlashAttribute("toast", ex.getMessage());
        }
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        return "redirect:/admin/orders";
    }

    private void populateProductListModel(Model model, int page, String keyword, String brand, String stock, String sort) {
        String normalizedKeyword = normalizeText(keyword);
        String normalizedBrand = normalizeText(brand);
        String normalizedStock = normalizeStock(stock);
        String normalizedSort = normalizeSort(sort);

        List<String> brands = productRepository.findAllNamesOrdered().stream()
                .map(this::extractBrand)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .toList();

        Long keywordId = parseKeywordId(normalizedKeyword);
        int safeRequestedPage = Math.max(page, 0);
        Integer minStock = resolveMinStock(normalizedStock);
        Integer maxStock = resolveMaxStock(normalizedStock);
        Sort requestedSort = Objects.requireNonNull(resolveAdminSort(normalizedSort));

        List<Product> products;
        int totalProducts;
        int totalPages;
        int safePage;

        if (normalizedBrand == null) {
            Page<Product> productPage = productRepository.findAdminProducts(
                    normalizedKeyword,
                    keywordId,
                    minStock,
                    maxStock,
                    PageRequest.of(safeRequestedPage, ADMIN_PAGE_SIZE, requestedSort));
            if (productPage.isEmpty() && safeRequestedPage > 0 && productPage.getTotalPages() > 0) {
                productPage = productRepository.findAdminProducts(
                        normalizedKeyword,
                        keywordId,
                        minStock,
                        maxStock,
                        PageRequest.of(productPage.getTotalPages() - 1, ADMIN_PAGE_SIZE, requestedSort));
            }
            products = productPage.getContent();
            totalProducts = (int) productPage.getTotalElements();
            totalPages = productPage.getTotalPages();
            safePage = totalPages == 0 ? 0 : Math.max(0, Math.min(productPage.getNumber(), totalPages - 1));
        } else {
            List<Product> filteredProducts = productRepository.findAllAdminProducts(
                    normalizedKeyword,
                    keywordId,
                    minStock,
                    maxStock).stream()
                    .filter(product -> normalizedBrand.equalsIgnoreCase(extractBrand(product)))
                    .toList();
            List<Product> sortedProducts = applyAdminSort(filteredProducts, normalizedSort);

            totalProducts = sortedProducts.size();
            totalPages = totalProducts == 0 ? 0 : (int) Math.ceil((double) totalProducts / ADMIN_PAGE_SIZE);
            safePage = totalPages == 0 ? 0 : Math.max(0, Math.min(safeRequestedPage, totalPages - 1));
            int fromIndex = Math.min(safePage * ADMIN_PAGE_SIZE, totalProducts);
            int toIndex = Math.min(fromIndex + ADMIN_PAGE_SIZE, totalProducts);
            products = sortedProducts.subList(fromIndex, toIndex);
        }

        model.addAttribute("products", products);
        model.addAttribute("currentPage", safePage);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("totalProducts", totalProducts);
        model.addAttribute("brands", brands);
        addProductListState(model, safePage, normalizedKeyword, normalizedBrand, normalizedStock, normalizedSort);
    }

    private List<Product> applyAdminSort(List<Product> products, String sort) {
        List<Product> sorted = new java.util.ArrayList<>(products);
        if ("id_desc".equals(sort)) {
            sorted.sort((left, right) -> compareByIdDescending(left, right));
        } else if ("name_asc".equals(sort)) {
            sorted.sort((left, right) -> compareByNameAscending(left, right));
        } else if ("name_desc".equals(sort)) {
            sorted.sort((left, right) -> compareByNameDescending(left, right));
        } else if ("price_asc".equals(sort)) {
            sorted.sort((left, right) -> compareByPriceAscending(left, right));
        } else if ("price_desc".equals(sort)) {
            sorted.sort((left, right) -> compareByPriceDescending(left, right));
        } else if ("stock_asc".equals(sort)) {
            sorted.sort((left, right) -> compareByStockAscending(left, right));
        } else if ("stock_desc".equals(sort)) {
            sorted.sort((left, right) -> compareByStockDescending(left, right));
        } else {
            sorted.sort((left, right) -> compareByIdAscending(left, right));
        }
        return sorted;
    }

    private String extractBrand(Product product) {
        return extractBrand(product != null ? product.getName() : null);
    }

    private String extractBrand(String productName) {
        String name = normalizeText(productName);
        if (name == null) {
            return "Other";
        }
        String firstToken = name.split("\\s+")[0];
        return switch (firstToken.toLowerCase(Locale.ROOT)) {
            case "iphone" -> "Apple";
            case "samsung" -> "Samsung";
            case "xiaomi" -> "Xiaomi";
            case "oppo" -> "OPPO";
            case "sony" -> "Sony";
            case "huawei" -> "Huawei";
            case "google" -> "Google";
            default -> capitalize(firstToken);
        };
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

    private Sort resolveAdminSort(String sort) {
        if (sort == null) {
            return Sort.by(Sort.Order.asc("name").ignoreCase());
        }
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

    private String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return "Other";
        }
        String lower = value.toLowerCase(Locale.ROOT);
        return Character.toUpperCase(lower.charAt(0)) + lower.substring(1);
    }

    private String normalizeText(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeNameForSort(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }

    private int compareByIdAscending(Product left, Product right) {
        return Long.compare(safeIdForAscendingSort(left), safeIdForAscendingSort(right));
    }

    private int compareByIdDescending(Product left, Product right) {
        return Long.compare(safeIdForAscendingSort(right), safeIdForAscendingSort(left));
    }

    private int compareByNameAscending(Product left, Product right) {
        return normalizeNameForSort(left != null ? left.getName() : null)
                .compareTo(normalizeNameForSort(right != null ? right.getName() : null));
    }

    private int compareByNameDescending(Product left, Product right) {
        return compareByNameAscending(right, left);
    }

    private int compareByPriceAscending(Product left, Product right) {
        return Double.compare(safePriceForAscendingSort(left), safePriceForAscendingSort(right));
    }

    private int compareByPriceDescending(Product left, Product right) {
        return Double.compare(safePriceForAscendingSort(right), safePriceForAscendingSort(left));
    }

    private int compareByStockAscending(Product left, Product right) {
        return Integer.compare(safeStockForAscendingSort(left), safeStockForAscendingSort(right));
    }

    private int compareByStockDescending(Product left, Product right) {
        return Integer.compare(safeStockForAscendingSort(right), safeStockForAscendingSort(left));
    }

    private long safeIdForAscendingSort(Product product) {
        Long id = product != null ? product.getId() : null;
        return id != null ? id : Long.MAX_VALUE;
    }

    private double safePriceForAscendingSort(Product product) {
        Double price = product != null ? product.getPrice() : null;
        return price != null ? price : Double.MAX_VALUE;
    }

    private int safeStockForAscendingSort(Product product) {
        Integer stock = product != null ? product.getStock() : null;
        return stock != null ? stock : Integer.MAX_VALUE;
    }

    private String normalizeStock(String stock) {
        return switch (stock) {
            case "in_stock", "low_stock", "out_of_stock" -> stock;
            default -> "all";
        };
    }

    private String normalizeSort(String sort) {
        return switch (sort) {
            case "id_desc", "name_asc", "name_desc", "price_asc", "price_desc", "stock_asc", "stock_desc" -> sort;
            default -> "name_asc";
        };
    }

    private void addProductListState(Model model, int page, String keyword, String brand, String stock, String sort) {
        model.addAttribute("currentPage", Math.max(page, 0));
        model.addAttribute("keyword", normalizeText(keyword));
        model.addAttribute("brand", normalizeText(brand));
        model.addAttribute("stock", normalizeStock(stock));
        model.addAttribute("sort", normalizeSort(sort));
    }

    private void addProductListState(RedirectAttributes redirectAttributes, int page, String keyword, String brand, String stock, String sort) {
        redirectAttributes.addAttribute("page", Math.max(page, 0));
        String normalizedKeyword = normalizeText(keyword);
        String normalizedBrand = normalizeText(brand);
        String normalizedStock = normalizeStock(stock);
        String normalizedSort = normalizeSort(sort);

        if (normalizedKeyword != null) {
            redirectAttributes.addAttribute("keyword", normalizedKeyword);
        }
        if (normalizedBrand != null) {
            redirectAttributes.addAttribute("brand", normalizedBrand);
        }
        if (!"all".equals(normalizedStock)) {
            redirectAttributes.addAttribute("stock", normalizedStock);
        }
        if (!"name_asc".equals(normalizedSort)) {
            redirectAttributes.addAttribute("sort", normalizedSort);
        }
    }

    private void normalizeAndValidateProduct(Product product) {
        product.setName(normalizeRequiredField(product.getName(), "Product name is required.", "Product name is too long.", 160));
        product.setImageUrl(normalizeOptionalField(product.getImageUrl(), "Image URL is too long.", 255));
        product.setOs(normalizeOptionalField(product.getOs(), "OS is too long.", 80));
        product.setRam(normalizeOptionalField(product.getRam(), "RAM is too long.", 80));
        product.setChipset(normalizeOptionalField(product.getChipset(), "Chipset is too long.", 120));
        product.setSpeed(normalizeOptionalField(product.getSpeed(), "Frequency is too long.", 80));
        product.setStorage(normalizeOptionalField(product.getStorage(), "Storage is too long.", 80));
        product.setSize(normalizeOptionalField(product.getSize(), "Screen size is too long.", 80));
        product.setResolution(normalizeOptionalField(product.getResolution(), "Resolution is too long.", 120));
        product.setBattery(normalizeOptionalField(product.getBattery(), "Battery is too long.", 80));
        product.setCharging(normalizeOptionalField(product.getCharging(), "Charging power is too long.", 80));
        product.setDescription(normalizeOptionalField(product.getDescription(), "Description is too long.", 1000));

        Double price = product.getPrice();
        if (price == null || price < 0) {
            throw new IllegalArgumentException("Price must be zero or greater.");
        }

        Integer stock = product.getStock();
        if (stock == null || stock < 0) {
            throw new IllegalArgumentException("Stock must be zero or greater.");
        }

        String imageUrl = product.getImageUrl();
        if (imageUrl != null
                && !(imageUrl.startsWith("/") || imageUrl.startsWith("https://"))) {
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

    private String normalizeAdminReturnUrl(String returnUrl) {
        if (returnUrl == null) {
            return null;
        }
        String trimmed = returnUrl.trim();
        if (trimmed.isBlank() || !trimmed.startsWith("/") || trimmed.startsWith("//")) {
            return null;
        }
        return trimmed;
    }

    private String buildAdminProductsUrl(int page, String keyword, String brand, String stock, String sort) {
        StringBuilder url = new StringBuilder("/admin/products");
        String separator = "?";

        url.append(separator).append("page=").append(Math.max(page, 0));
        separator = "&";

        String normalizedKeyword = normalizeText(keyword);
        if (normalizedKeyword != null) {
            url.append(separator).append("keyword=").append(normalizedKeyword);
        }

        String normalizedBrand = normalizeText(brand);
        if (normalizedBrand != null) {
            url.append(separator).append("brand=").append(normalizedBrand);
        }

        String normalizedStock = normalizeStock(stock);
        if (!"all".equals(normalizedStock)) {
            url.append(separator).append("stock=").append(normalizedStock);
        }

        String normalizedSort = normalizeSort(sort);
        if (!"name_asc".equals(normalizedSort)) {
            url.append(separator).append("sort=").append(normalizedSort);
        }

        return url.toString();
    }
}
