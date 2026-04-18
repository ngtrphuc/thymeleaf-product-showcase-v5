package io.github.ngtrphuc.smartphone_shop.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.OrderValidationException;
import io.github.ngtrphuc.smartphone_shop.common.exception.UnauthorizedActionException;
import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.Order;
import io.github.ngtrphuc.smartphone_shop.model.OrderItem;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.OrderRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;

@Service
public class OrderService {

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "pending", "processing", "shipped", "delivered", "cancelled");
    private static final Set<String> ALLOWED_PAYMENT_METHODS = Set.of(
            "CASH_ON_DELIVERY", "BANK_TRANSFER", "PAYPAY", "MASTERCARD");
    private static final Set<String> ALLOWED_PAYMENT_PLANS = Set.of(
            "FULL_PAYMENT", "INSTALLMENT");
    private static final int DEFAULT_INSTALLMENT_MONTHS = 24;
    private static final Set<Integer> ALLOWED_INSTALLMENT_MONTHS = Set.of(6, 12, DEFAULT_INSTALLMENT_MONTHS);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9+()\\-\\s]{6,30}$");

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
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

        Map<Long, Integer> requestedQuantities = extractCartQuantities(cartItems);
        Map<Long, Product> lockedProducts = loadProductsForUpdate(requestedQuantities.keySet());
        validateRequestedStock(requestedQuantities, lockedProducts);

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
        double totalAmount = calculateOrderTotal(cartItems, lockedProducts);
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

        for (CartItem item : cartItems) {
            Product product = lockedProducts.get(item.getId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProductId(item.getId());
            orderItem.setProductName(product != null ? product.getName() : item.getName());
            orderItem.setPrice(currentProductPrice(product));
            orderItem.setQuantity(item.getQuantity());
            order.getItems().add(orderItem);
        }

        applyStockDelta(lockedProducts, requestedQuantities, -1, false);
        return orderRepository.save(order);
    }

    @Transactional
    public Order createOrder(String userEmail, String name, String phone,
            String address, List<CartItem> cartItems) {
        return createOrder(userEmail, name, phone, address, cartItems, "CASH_ON_DELIVERY", null);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByUser(String email) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<Order> getAdminOrdersPage(int page, int limit) {
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
    public List<Order> getRecentOrders(int limit) {
        return orderRepository.findRecentOrders(PageRequest.of(0, limit));
    }

    @Transactional(readOnly = true)
    public List<Order> getRecentOrders(int page, int limit) {
        int safePage = Math.max(0, page);
        int safeLimit = Math.max(1, limit);
        return orderRepository.findRecentOrders(PageRequest.of(safePage, safeLimit));
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

    @Transactional
    public void updateStatus(long orderId, String newStatus) {
        Order order = orderRepository.findByIdWithItemsForUpdate(orderId)
                .orElseThrow(() -> new OrderValidationException("Order not found."));

        String oldStatus = normalizeStatus(order.getStatus());
        String targetStatus = normalizeStatus(newStatus);
        if (!ALLOWED_STATUSES.contains(targetStatus)) {
            throw new OrderValidationException("Unsupported order status.");
        }
        if (Objects.equals(oldStatus, targetStatus)) {
            return;
        }

        Map<Long, Integer> itemQuantities = extractOrderQuantities(order.getItems());
        if ("cancelled".equals(oldStatus) && !"cancelled".equals(targetStatus)) {
            Map<Long, Product> lockedProducts = loadProductsForUpdate(itemQuantities.keySet());
            validateRequestedStock(itemQuantities, lockedProducts);
            applyStockDelta(lockedProducts, itemQuantities, -1, false);
        } else if (!"cancelled".equals(oldStatus) && "cancelled".equals(targetStatus)) {
            Map<Long, Product> lockedProducts = loadProductsForUpdate(itemQuantities.keySet());
            applyStockDelta(lockedProducts, itemQuantities, 1, true);
        }

        order.setStatus(targetStatus);
        orderRepository.save(order);
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

        String status = normalizeStatus(order.getStatus());
        if (!"pending".equals(status) && !"processing".equals(status)) {
            return false;
        }

        Map<Long, Integer> itemQuantities = extractOrderQuantities(order.getItems());
        Map<Long, Product> lockedProducts = loadProductsForUpdate(itemQuantities.keySet());
        applyStockDelta(lockedProducts, itemQuantities, 1, true);
        order.setStatus("cancelled");
        orderRepository.save(order);
        return true;
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

    private Map<Long, Integer> extractCartQuantities(List<CartItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (CartItem item : items) {
            Long productId = item.getId();
            if (productId == null) {
                continue;
            }
            int quantity = item.getQuantity();
            if (quantity <= 0) {
                throw new OrderValidationException("Invalid item quantity in cart.");
            }
            quantities.put(productId, quantities.getOrDefault(productId, 0) + quantity);
        }
        if (quantities.isEmpty()) {
            throw new OrderValidationException("Your cart is empty.");
        }
        return quantities;
    }

    private Map<Long, Integer> extractOrderQuantities(List<OrderItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItem item : items) {
            Long productId = item.getProductId();
            if (productId == null) {
                continue;
            }
            quantities.put(productId, quantities.getOrDefault(productId, 0) + item.getQuantity());
        }
        return quantities;
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

    private void validateRequestedStock(Map<Long, Integer> requestedQuantities, Map<Long, Product> products) {
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

    private void applyStockDelta(Map<Long, Product> products, Map<Long, Integer> quantities,
            int direction, boolean allowMissingProducts) {
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
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return "pending";
        }
        return status.trim().toLowerCase(Locale.ROOT);
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

    private double calculateOrderTotal(List<CartItem> cartItems, Map<Long, Product> lockedProducts) {
        return cartItems.stream()
                .mapToDouble(item -> currentProductPrice(lockedProducts.get(item.getId())) * item.getQuantity())
                .sum();
    }

    private double currentProductPrice(Product product) {
        if (product == null) {
            throw new OrderValidationException("One of the products in your order is no longer available.");
        }
        Double price = product.getPrice();
        if (price == null || price < 0) {
            throw new OrderValidationException(product.getName() + " has an invalid price.");
        }
        return price;
    }
}
