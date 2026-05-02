package io.github.ngtrphuc.smartphone_shop.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.time.LocalDateTime;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.common.exception.UnauthorizedActionException;
import io.github.ngtrphuc.smartphone_shop.common.support.CacheKeys;
import io.github.ngtrphuc.smartphone_shop.common.support.ValidationConstants;
import io.github.ngtrphuc.smartphone_shop.event.OrderCreatedEvent;
import io.github.ngtrphuc.smartphone_shop.event.OrderStatusChangedEvent;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderItem;
import io.github.ngtrphuc.smartphone_shop.model.OrderStatus;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductVariantRepository;

@Service
public class OrderService {

    private static final String CATALOG_PUBLIC_CACHE = "catalogPublic";
    private static final String PRODUCT_DETAIL_PUBLIC_CACHE = "productDetailPublic";
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of(
            "CASH_ON_DELIVERY", "BANK_TRANSFER", "PAYPAY", "MASTERCARD");
    private static final Set<String> ALLOWED_PAYMENT_PLANS = Set.of(
            "FULL_PAYMENT", "INSTALLMENT");
    private static final int DEFAULT_INSTALLMENT_MONTHS = 24;
    private static final Set<Integer> ALLOWED_INSTALLMENT_MONTHS = Set.of(6, 12, DEFAULT_INSTALLMENT_MONTHS);
    private static final Pattern PHONE_PATTERN = ValidationConstants.PHONE_PATTERN;

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductCommerceService productCommerceService;
    private final CacheManager cacheManager;
    private final ApplicationEventPublisher eventPublisher;

    @Autowired
    public OrderService(OrderRepository orderRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductCommerceService productCommerceService,
            ApplicationEventPublisher eventPublisher,
            @Nullable CacheManager cacheManager) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productCommerceService = productCommerceService;
        this.eventPublisher = eventPublisher;
        this.cacheManager = cacheManager;
    }

    /**
     * Backward-compatible constructor for existing unit tests.
     */
    public OrderService(OrderRepository orderRepository,
            ProductRepository productRepository,
            ApplicationEventPublisher eventPublisher,
            @Nullable CacheManager cacheManager) {
        this(orderRepository, productRepository, null, new ProductCommerceService(null, null, null, null, null), eventPublisher, cacheManager);
    }

    @Transactional
    public Order createOrder(String userEmail, String name, String phone,
            String address, List<CartItem> cartItems,
            String paymentMethod, String paymentDetail) {
        return createOrder(
                userEmail, name, phone, address, cartItems,
                paymentMethod, paymentDetail, "FULL_PAYMENT", null);
    }

    @Transactional
    public Order createOrder(String userEmail, String name, String phone,
            String address, List<CartItem> cartItems,
            String paymentMethod, String paymentDetail,
            String paymentPlan, Integer installmentMonths) {
        if (userEmail == null || userEmail.isBlank()) {
            throw new OrderValidationException("Please log in before placing an order.");
        }
        if (cartItems == null || cartItems.isEmpty()) {
            throw new OrderValidationException("Your cart is empty.");
        }

        String normalizedPaymentMethod = normalizePaymentMethod(paymentMethod);
        String normalizedPaymentDetail = normalizePaymentDetail(normalizedPaymentMethod, paymentDetail);
        String normalizedPaymentPlan = normalizePaymentPlan(paymentPlan);
        Integer normalizedInstallmentMonths = normalizeInstallmentMonths(
                normalizedPaymentPlan,
                installmentMonths,
                normalizedPaymentMethod);

        List<OrderLine> lines = resolveOrderLines(cartItems);
        if (lines.isEmpty()) {
            throw new OrderValidationException("Your cart is empty.");
        }

        Map<Long, Integer> requestedQuantities = aggregateVariantQuantities(lines);
        Map<Long, Integer> requestedProductQuantities = aggregateProductQuantities(lines);
        Map<Long, ProductVariant> lockedVariants = loadVariantsForUpdate(requestedQuantities.keySet());
        Map<Long, Product> lockedProducts = loadProductsForUpdate(requestedProductQuantities.keySet());
        validateRequestedStock(requestedQuantities, lockedVariants);
        validateRequestedProductStock(requestedProductQuantities, lockedProducts);

        String normalizedName = normalizeRequiredText(
                name, "Customer name is required.", "Customer name is too long.", 120);
        String normalizedPhone = normalizeRequiredText(
                phone, "Phone number is required.", "Phone number is too long.", 30);
        String normalizedAddress = normalizeRequiredText(
                address, "Shipping address is required.", "Shipping address is too long.", 255);
        validatePhoneNumber(normalizedPhone);

        Order order = new Order();
        order.setUserEmail(userEmail);
        order.setCustomerName(normalizedName);
        order.setPhoneNumber(normalizedPhone);
        order.setShippingAddress(normalizedAddress);
        order.setPaymentMethod(normalizedPaymentMethod);
        order.setPaymentDetail(normalizedPaymentDetail);

        double totalAmount = calculateOrderTotal(lines);
        order.setTotalAmount(totalAmount);
        order.setPaymentPlan(normalizedPaymentPlan);
        if ("INSTALLMENT".equals(normalizedPaymentPlan)) {
            order.setInstallmentMonths(normalizedInstallmentMonths);
            order.setInstallmentMonthlyAmount(Math.round(totalAmount / normalizedInstallmentMonths));
        } else {
            order.setInstallmentMonths(null);
            order.setInstallmentMonthlyAmount(null);
        }
        order.setStatus("pending");

        for (OrderLine line : lines) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(line.product().getId());
            orderItem.setProductName(line.product().getName());
            if (line.variant() != null) {
                orderItem.setVariantId(line.variant().getId());
                orderItem.setVariantSku(line.variant().getSku());
                orderItem.setVariantLabel(line.variant().label());
            }
            orderItem.setPrice(line.unitPrice());
            orderItem.setQuantity(line.quantity());
            order.getItems().add(orderItem);
        }

        applyVariantStockDelta(lockedVariants, requestedQuantities, -1, false);
        applyProductStockDelta(lockedProducts, requestedProductQuantities, -1, false);
        syncProductStockForVariants(lockedVariants.values());

        Order saved = orderRepository.save(order);
        evictStorefrontCaches(lines.stream().map(line -> line.product().getId()).toList());
        publishOrderCreated(saved);
        return saved;
    }

    @Transactional
    public Order createOrder(String userEmail, String name, String phone,
            String address, List<CartItem> cartItems) {
        return createOrder(userEmail, name, phone, address, cartItems, "CASH_ON_DELIVERY", null);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(String email, int page, int pageSize) {
        int safePage = Math.max(0, page);
        int safeLimit = Math.max(1, Math.min(pageSize, 50));
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(email, PageRequest.of(safePage, safeLimit));
    }

    @Deprecated(since = "5.1", forRemoval = true)
    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(String email) {
        return getOrdersByUser(email, 0, 50);
    }

    @Transactional(readOnly = true)
    public long countOrdersByUser(String email) {
        return orderRepository.countByUserEmail(email);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getAdminOrdersPage(int page, int limit) {
        return loadOrdersPageWithItems(page, limit);
    }

    @Transactional(readOnly = true)
    public List<Order> getRecentOrders(int limit) {
        return loadOrdersPageWithItems(0, limit);
    }

    @Transactional(readOnly = true)
    public List<Order> getRecentOrders(int page, int limit) {
        return loadOrdersPageWithItems(page, limit);
    }

    private List<Order> loadOrdersPageWithItems(int page, int limit) {
        int safePage = Math.max(0, page);
        int safeLimit = Math.max(1, limit);
        List<Long> orderIds = orderRepository.findOrderIdsByCreatedAtDesc(PageRequest.of(safePage, safeLimit))
                .getContent();
        if (orderIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Integer> sortOrder = new LinkedHashMap<>();
        for (int index = 0; index < orderIds.size(); index++) {
            sortOrder.put(orderIds.get(index), index);
        }

        return orderRepository.findAllWithItemsByIdIn(orderIds).stream()
                .sorted(Comparator.comparingInt(order -> sortOrder.getOrDefault(order.getId(), Integer.MAX_VALUE)))
                .toList();
    }

    @Transactional(readOnly = true)
    public double getTotalRevenue() {
        Double result = orderRepository.sumRevenueExcludingCancelled();
        return result != null ? result : 0.0;
    }

    @Transactional(readOnly = true)
    public long getTotalItemsSold() {
        Long result = orderRepository.sumItemsSoldExcludingCancelled();
        return result != null ? result : 0L;
    }

    @Transactional(readOnly = true)
    public long countOrders() {
        return orderRepository.count();
    }

    @Transactional(readOnly = true)
    public long countDeliveredOrdersByUser(String email) {
        return orderRepository.countDeliveredByUserEmail(email);
    }

    @Transactional(readOnly = true)
    public long countPendingOrdersByUser(String email) {
        return orderRepository.countPendingByUserEmail(email);
    }

    @Transactional
    public void updateStatus(long orderId, String newStatus) {
        Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
                .orElseThrow(() -> new OrderValidationException("Order not found."));
        OrderStatus targetStatus = parseStatus(newStatus);
        applyStatusTransition(order, targetStatus);
    }

    @Transactional
    public void shipOrder(long orderId, String trackingNumber, String carrier) {
        String normalizedTrackingNumber = normalizeRequiredText(
                trackingNumber,
                "Tracking number is required.",
                "Tracking number is too long.",
                100);
        String normalizedCarrier = normalizeRequiredText(
                carrier,
                "Tracking carrier is required.",
                "Tracking carrier is too long.",
                50);

        Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
                .orElseThrow(() -> new OrderValidationException("Order not found."));
        order.setTrackingNumber(normalizedTrackingNumber);
        order.setTrackingCarrier(normalizedCarrier);
        applyStatusTransition(order, OrderStatus.SHIPPED);
    }

    private void applyStatusTransition(Order order, OrderStatus targetStatus) {
        if (order == null) {
            throw new OrderValidationException("Order not found.");
        }

        OrderStatus currentStatus = parseStatus(order.getStatus());
        if (currentStatus == targetStatus) {
            return;
        }
        if (!currentStatus.canTransitionTo(targetStatus)) {
            throw new OrderValidationException(
                    "Cannot transition from " + currentStatus.value() + " to " + targetStatus.value() + ".");
        }

        Map<Long, Integer> variantQuantities = extractOrderVariantQuantities(order.getItems());
        Map<Long, Integer> productQuantities = extractOrderProductQuantities(order.getItems());
        boolean stockChanged = false;
        if (currentStatus == OrderStatus.CANCELLED && targetStatus != OrderStatus.CANCELLED) {
            Map<Long, ProductVariant> lockedVariants = loadVariantsForUpdate(variantQuantities.keySet());
            Map<Long, Product> lockedProducts = loadProductsForUpdate(productQuantities.keySet());
            validateRequestedStock(variantQuantities, lockedVariants);
            validateRequestedProductStock(productQuantities, lockedProducts);
            applyVariantStockDelta(lockedVariants, variantQuantities, -1, false);
            applyProductStockDelta(lockedProducts, productQuantities, -1, false);
            syncProductStockForVariants(lockedVariants.values());
            stockChanged = true;
        } else if (currentStatus != OrderStatus.CANCELLED && targetStatus == OrderStatus.CANCELLED) {
            Map<Long, ProductVariant> lockedVariants = loadVariantsForUpdate(variantQuantities.keySet());
            Map<Long, Product> lockedProducts = loadProductsForUpdate(productQuantities.keySet());
            applyVariantStockDelta(lockedVariants, variantQuantities, 1, true);
            applyProductStockDelta(lockedProducts, productQuantities, 1, true);
            syncProductStockForVariants(lockedVariants.values());
            stockChanged = true;
        }

        LocalDateTime now = LocalDateTime.now();
        switch (targetStatus) {
            case SHIPPED -> order.setShippedAt(now);
            case DELIVERED -> order.setDeliveredAt(now);
            case COMPLETED, REFUNDED -> order.setCompletedAt(now);
            default -> {
            }
        }

        order.setStatus(targetStatus.value());
        orderRepository.save(order);
        if (stockChanged) {
            evictStorefrontCaches(order.getItems().stream().map(OrderItem::getProductId).filter(Objects::nonNull).toList());
        }
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(),
                order.getOrderCode(),
                order.getUserEmail(),
                currentStatus.value(),
                targetStatus.value(),
                order.getTrackingNumber(),
                order.getTrackingCarrier()));
    }

    @Transactional
    public boolean cancelOrder(long orderId, String userEmail) {
        Optional<Order> optionalOrder = orderRepository.findByIdWithItemsForUpdate(orderId);
        if (optionalOrder.isEmpty()) {
            return false;
        }

        Order order = optionalOrder.get();
        if (!Objects.equals(order.getUserEmail(), userEmail)) {
            throw new UnauthorizedActionException("You do not have permission to cancel this order.");
        }

        OrderStatus status = parseStatus(order.getStatus());
        if (!status.isCancelableByCustomer()) {
            return false;
        }

        Map<Long, Integer> variantQuantities = extractOrderVariantQuantities(order.getItems());
        Map<Long, Integer> productQuantities = extractOrderProductQuantities(order.getItems());
        Map<Long, ProductVariant> lockedVariants = loadVariantsForUpdate(variantQuantities.keySet());
        Map<Long, Product> lockedProducts = loadProductsForUpdate(productQuantities.keySet());
        applyVariantStockDelta(lockedVariants, variantQuantities, 1, true);
        applyProductStockDelta(lockedProducts, productQuantities, 1, true);
        syncProductStockForVariants(lockedVariants.values());

        order.setStatus(OrderStatus.CANCELLED.value());
        orderRepository.save(order);
        evictStorefrontCaches(order.getItems().stream().map(OrderItem::getProductId).filter(Objects::nonNull).toList());
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                order.getId(),
                order.getOrderCode(),
                order.getUserEmail(),
                status.value(),
                OrderStatus.CANCELLED.value(),
                order.getTrackingNumber(),
                order.getTrackingCarrier()));
        return true;
    }

    private List<OrderLine> resolveOrderLines(List<CartItem> cartItems) {
        List<Long> productIds = cartItems.stream()
                .map(CartItem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productRepository.findAllByIdInForUpdate(productIds).stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<OrderLine> lines = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            if (cartItem == null || cartItem.getId() == null) {
                continue;
            }
            Product product = productMap.get(cartItem.getId());
            if (product == null) {
                throw new OrderValidationException("One of the products in your cart is no longer available.");
            }
            ProductVariant variant = productCommerceService.resolveVariantOrDefault(product, cartItem.getVariantId());
            int quantity = Math.max(0, cartItem.getQuantity());
            if (quantity <= 0) {
                throw new OrderValidationException("Invalid item quantity in cart.");
            }
            double unitPrice = variant != null
                    ? productCommerceService.resolveEffectivePrice(product, variant)
                    : Optional.ofNullable(product.getPrice()).orElse(0.0);
            lines.add(new OrderLine(product, variant, quantity, unitPrice));
        }
        return lines;
    }

    private Map<Long, Integer> aggregateVariantQuantities(List<OrderLine> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : lines) {
            if (line.variant() == null) {
                continue;
            }
            Long variantId = line.variant().getId();
            if (variantId == null) {
                continue;
            }
            quantities.put(variantId, quantities.getOrDefault(variantId, 0) + line.quantity());
        }
        return quantities;
    }

    private Map<Long, Integer> aggregateProductQuantities(List<OrderLine> lines) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderLine line : lines) {
            if (line.variant() != null || line.product().getId() == null) {
                continue;
            }
            Long productId = line.product().getId();
            quantities.put(productId, quantities.getOrDefault(productId, 0) + line.quantity());
        }
        return quantities;
    }

    private Map<Long, Integer> extractOrderVariantQuantities(List<OrderItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Long variantId = item.getVariantId();
            if (variantId == null) {
                continue;
            }
            quantities.put(variantId, quantities.getOrDefault(variantId, 0) + item.getQuantity());
        }
        return quantities;
    }

    private Map<Long, Integer> extractOrderProductQuantities(List<OrderItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item.getVariantId() != null || item.getProductId() == null) {
                continue;
            }
            Long productId = item.getProductId();
            quantities.put(productId, quantities.getOrDefault(productId, 0) + item.getQuantity());
        }
        return quantities;
    }

    private Map<Long, ProductVariant> loadVariantsForUpdate(Iterable<Long> variantIds) {
        if (productVariantRepository == null) {
            return Map.of();
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : variantIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productVariantRepository.findAllByIdInForUpdate(ids).stream()
                .filter(variant -> variant.getId() != null)
                .collect(Collectors.toMap(ProductVariant::getId, variant -> variant));
    }

    private Map<Long, Product> loadProductsForUpdate(Iterable<Long> productIds) {
        Set<Long> ids = new LinkedHashSet<>();
        for (Long id : productIds) {
            if (id != null) {
                ids.add(id);
            }
        }
        if (ids.isEmpty()) {
            return Map.of();
        }
        return productRepository.findAllByIdInForUpdate(ids).stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product));
    }

    private void validateRequestedStock(Map<Long, Integer> requestedQuantities, Map<Long, ProductVariant> variants) {
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            ProductVariant variant = variants.get(entry.getKey());
            if (variant == null) {
                throw new OrderValidationException("One of the products in your cart is no longer available.");
            }

            int requested = entry.getValue();
            int available = Optional.ofNullable(variant.getStock()).orElse(0);
            if (requested > available) {
                String productName = variant.getProduct() != null ? variant.getProduct().getName() : "Product";
                throw new OrderValidationException(productName + " variant " + variant.label() + " only has " + available + " item(s) left in stock.");
            }
        }
    }

    private void validateRequestedProductStock(Map<Long, Integer> requestedQuantities, Map<Long, Product> products) {
        for (Map.Entry<Long, Integer> entry : requestedQuantities.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) {
                throw new OrderValidationException("One of the products in your cart is no longer available.");
            }
            int requested = entry.getValue();
            int available = Optional.ofNullable(product.getStock()).orElse(0);
            if (requested > available) {
                throw new OrderValidationException(product.getName() + " only has " + available + " item(s) left in stock.");
            }
        }
    }

    private void applyVariantStockDelta(Map<Long, ProductVariant> variants,
            Map<Long, Integer> quantities,
            int direction,
            boolean allowMissingVariants) {
        if (quantities.isEmpty()) {
            return;
        }
        if (productVariantRepository == null) {
            if (allowMissingVariants) {
                return;
            }
            throw new OrderValidationException("One of the variants in this order is no longer available.");
        }
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            ProductVariant variant = variants.get(entry.getKey());
            if (variant == null) {
                if (allowMissingVariants) {
                    continue;
                }
                throw new OrderValidationException("One of the variants in this order is no longer available.");
            }

            int currentStock = Optional.ofNullable(variant.getStock()).orElse(0);
            int nextStock = currentStock + (entry.getValue() * direction);
            if (nextStock < 0) {
                String productName = variant.getProduct() != null ? variant.getProduct().getName() : "Product";
                throw new OrderValidationException(productName + " variant " + variant.label() + " does not have enough stock to continue.");
            }
            variant.setStock(nextStock);
        }
        List<ProductVariant> variantsToSave = new ArrayList<>();
        for (ProductVariant variant : variants.values()) {
            if (variant != null) {
                variantsToSave.add(variant);
            }
        }
        if (!variantsToSave.isEmpty()) {
            productVariantRepository.saveAll(variantsToSave);
        }
    }

    private void applyProductStockDelta(Map<Long, Product> products,
            Map<Long, Integer> quantities,
            int direction,
            boolean allowMissingProducts) {
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Product product = products.get(entry.getKey());
            if (product == null) {
                if (allowMissingProducts) {
                    continue;
                }
                throw new OrderValidationException("One of the products in this order is no longer available.");
            }
            int currentStock = Optional.ofNullable(product.getStock()).orElse(0);
            int nextStock = currentStock + (entry.getValue() * direction);
            if (nextStock < 0) {
                throw new OrderValidationException(product.getName() + " does not have enough stock to continue.");
            }
            product.setStock(nextStock);
        }
        if (!products.isEmpty()) {
            List<Product> productsToSave = new ArrayList<>();
            for (Product product : products.values()) {
                if (product != null) {
                    productsToSave.add(product);
                }
            }
            if (!productsToSave.isEmpty()) {
                productRepository.saveAll(productsToSave);
            }
        }
    }

    private void syncProductStockForVariants(Iterable<ProductVariant> variants) {
        Set<Long> productIds = new LinkedHashSet<>();
        for (ProductVariant variant : variants) {
            if (variant != null && variant.getProduct() != null && variant.getProduct().getId() != null) {
                productIds.add(variant.getProduct().getId());
            }
        }
        if (productIds.isEmpty()) {
            return;
        }

        for (Long productId : productIds) {
            if (productId == null) {
                continue;
            }
            Product product = productRepository.findById(productId).orElse(null);
            if (product == null) {
                continue;
            }
            List<ProductVariant> productVariants = productVariantRepository.findByProductIdOrderByActiveDescIdAsc(productId);
            int totalStock = productVariants.stream()
                    .filter(ProductVariant::isActive)
                    .map(ProductVariant::getStock)
                    .filter(Objects::nonNull)
                    .mapToInt(Integer::intValue)
                    .sum();
            product.setStock(totalStock);
            ProductVariant first = productVariants.stream().filter(ProductVariant::isActive).findFirst().orElse(null);
            if (first != null && first.effectivePrice() != null) {
                product.setPrice(first.effectivePrice());
            }
            productRepository.save(product);
        }
    }

    private String normalizePaymentMethod(String raw) {
        if (raw == null || raw.isBlank()) {
            return "CASH_ON_DELIVERY";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAYMENT_METHODS.contains(upper)) {
            throw new OrderValidationException("Invalid payment method.");
        }
        return upper;
    }

    private String normalizePaymentDetail(String method, String detail) {
        if (!"BANK_TRANSFER".equals(method)) {
            return null;
        }
        String normalized = detail == null ? "" : detail.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new OrderValidationException("Bank account details are required for Bank Transfer.");
        }
        if (normalized.length() > 200) {
            throw new OrderValidationException("Bank account details are too long.");
        }
        return normalized;
    }

    private String normalizePaymentPlan(String raw) {
        if (raw == null || raw.isBlank()) {
            return "FULL_PAYMENT";
        }
        String upper = raw.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_PAYMENT_PLANS.contains(upper)) {
            throw new OrderValidationException("Invalid payment plan.");
        }
        return upper;
    }

    private Integer normalizeInstallmentMonths(String paymentPlan, Integer rawMonths, String paymentMethod) {
        if (!"INSTALLMENT".equals(paymentPlan)) {
            return null;
        }
        if ("CASH_ON_DELIVERY".equals(paymentMethod)) {
            throw new OrderValidationException("Installment is not available for Cash on Delivery.");
        }
        int resolvedMonths = rawMonths == null ? DEFAULT_INSTALLMENT_MONTHS : rawMonths;
        if (!ALLOWED_INSTALLMENT_MONTHS.contains(resolvedMonths)) {
            throw new OrderValidationException("Unsupported installment period.");
        }
        return resolvedMonths;
    }

    private OrderStatus parseStatus(String status) {
        String normalized = status == null || status.isBlank() ? OrderStatus.PENDING.value() : status;
        try {
            return OrderStatus.from(normalized);
        } catch (IllegalArgumentException ex) {
            throw new OrderValidationException("Unsupported order status.");
        }
    }

    private String normalizeRequiredText(String value, String emptyMessage, String tooLongMessage, int maxLength) {
        String normalized = value == null ? "" : value.trim().replaceAll("\\s+", " ");
        if (normalized.isBlank()) {
            throw new OrderValidationException(emptyMessage);
        }
        if (normalized.length() > maxLength) {
            throw new OrderValidationException(tooLongMessage);
        }
        return normalized;
    }

    private void validatePhoneNumber(String phone) {
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new OrderValidationException("Phone number format is invalid.");
        }
    }

    private double calculateOrderTotal(List<OrderLine> lines) {
        return lines.stream()
                .mapToDouble(line -> line.unitPrice() * line.quantity())
                .sum();
    }

    private void evictStorefrontCaches(Iterable<Long> productIds) {
        if (cacheManager == null) {
            return;
        }

        Cache catalogCache = cacheManager.getCache(CATALOG_PUBLIC_CACHE);
        if (catalogCache != null) {
            catalogCache.clear();
        }

        Cache detailCache = cacheManager.getCache(PRODUCT_DETAIL_PUBLIC_CACHE);
        if (detailCache == null) {
            return;
        }
        for (Long productId : productIds) {
            if (productId == null) {
                continue;
            }
            String detailKey = Objects.requireNonNull(
                    CacheKeys.productDetail(productId),
                    "Product detail cache key must not be null.");
            detailCache.evict(detailKey);
        }
    }

    private void publishOrderCreated(Order order) {
        if (order == null || order.getId() == null) {
            return;
        }
        eventPublisher.publishEvent(new OrderCreatedEvent(
                order.getId(),
                order.getUserEmail(),
                order.getOrderCode(),
                order.getTotalAmount(),
                order.getItemCount(),
                order.getPaymentMethod(),
                order.getPaymentPlan(),
                order.getCreatedAt()));
    }

    private record OrderLine(Product product, ProductVariant variant, int quantity, double unitPrice) {
    }
}
