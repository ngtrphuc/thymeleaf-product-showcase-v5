package io.github.ngtrphuc.smartphone_shop.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.Nullable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.CartItemEntity;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.ProductImage;
import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductImageRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductVariantRepository;
import jakarta.servlet.http.HttpSession;

@Service
public class CartService {

    public enum AddItemResult {
        ADDED,
        LIMIT_REACHED,
        UNAVAILABLE
    }

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductCommerceService productCommerceService;

    @Autowired
    public CartService(CartItemRepository cartItemRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ProductImageRepository productImageRepository,
            ProductCommerceService productCommerceService) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.productCommerceService = productCommerceService;
    }

    /**
     * Backward-compatible constructor for existing unit tests.
     */
    public CartService(CartItemRepository cartItemRepository,
            ProductRepository productRepository) {
        this(cartItemRepository, productRepository, null, null, new ProductCommerceService(null, null, null, null, null));
    }

    public List<CartItem> getSessionCart(@Nullable HttpSession session) {
        if (session == null) {
            return new ArrayList<>();
        }
        Object obj = session.getAttribute("cart");
        if (obj instanceof List<?> rawCart) {
            if (rawCart.stream().allMatch(CartItem.class::isInstance)) {
                List<CartItem> cart = rawCart.stream()
                        .map(CartItem.class::cast)
                        .collect(Collectors.toCollection(ArrayList::new));
                session.setAttribute("cart", cart);
                return cart;
            }
            List<CartItem> cart = new ArrayList<>();
            session.setAttribute("cart", cart);
            return cart;
        }
        if (obj == null) {
            List<CartItem> cart = new ArrayList<>();
            session.setAttribute("cart", cart);
            return cart;
        }
        List<CartItem> cart = new ArrayList<>();
        session.setAttribute("cart", cart);
        return cart;
    }

    public void syncCartCount(@Nullable HttpSession session, String email) {
        if (session == null) {
            return;
        }
        int count;
        if (isLoggedIn(email)) {
            count = getDbCartSnapshot(email)
                    .stream().mapToInt(CartItem::getQuantity).sum();
        } else {
            count = getSessionCart(session)
                    .stream().mapToInt(CartItem::getQuantity).sum();
        }
        session.setAttribute("cartCount", count);
    }

    @Transactional
    public void mergeSessionCartToDb(@Nullable HttpSession session, String email) {
        List<CartItem> sessionCart = getSessionCart(session);
        if (sessionCart.isEmpty()) {
            return;
        }
        List<Long> sessionProductIds = sessionCart.stream()
                .map(CartItem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productRepository.findAllByIdIn(sessionProductIds)
                .stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product));

        Map<String, CartItemEntity> existingByLineKey = new LinkedHashMap<>();
        List<CartItemEntity> dirtyEntities = new ArrayList<>();
        for (CartItemEntity existing : cartItemRepository.findByUserEmail(email)) {
            existingByLineKey.putIfAbsent(buildLineKey(existing.getVariantId(), existing.getProductId()), existing);
        }

        for (CartItem item : sessionCart) {
            if (item == null) {
                continue;
            }
            Product product = item.getId() != null ? productMap.get(item.getId()) : null;
            if (product == null) {
                continue;
            }
            LineContext line = resolveLine(product, item.getVariantId());
            if (!line.available()) {
                continue;
            }
            String lineKey = buildLineKey(line.variantId(), line.product().getId());
            CartItemEntity existing = existingByLineKey.get(lineKey);
            int requestedQty = Math.max(item.getQuantity(), 0);
            if (requestedQty <= 0) {
                continue;
            }

            if (existing != null) {
                int mergedQty = Math.min(existing.getQuantity() + requestedQty, line.stock());
                existing.setQuantity(Math.max(1, mergedQty));
                dirtyEntities.add(existing);
            } else {
                int initialQty = Math.min(requestedQty, line.stock());
                if (initialQty > 0) {
                    CartItemEntity created = new CartItemEntity(email, line.product().getId(), line.variantId(), initialQty);
                    dirtyEntities.add(created);
                    existingByLineKey.put(lineKey, created);
                }
            }
        }

        if (!dirtyEntities.isEmpty()) {
            cartItemRepository.saveAll(dirtyEntities);
        }

        if (session != null) {
            session.removeAttribute("cart");
        }
        normalizeUserCart(email);
        syncCartCount(session, email);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getDbCart(String email) {
        return getDbCartSnapshot(email);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getDbCartSnapshot(String email) {
        List<CartItemEntity> entities = cartItemRepository.findByUserEmail(email);
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = entities.stream()
                .map(CartItemEntity::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds)
                .stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p));

        List<Long> variantIds = entities.stream()
                .map(CartItemEntity::getVariantId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, ProductVariant> variantMap = productVariantRepository == null
                ? Map.of()
                : productVariantRepository.findAllByIdIn(variantIds)
                        .stream()
                        .filter(v -> v.getId() != null)
                        .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        Map<Long, String> primaryImageByProductId = buildPrimaryImageMap(productIds);

        List<CartItem> result = new ArrayList<>();
        for (CartItemEntity entity : entities) {
            Product product = productMap.get(entity.getProductId());
            ProductVariant variant = resolveVariant(product, entity.getVariantId(), variantMap);
            CartItem mapped = toCartItem(entity, product, variant, primaryImageByProductId.get(entity.getProductId()));
            if (mapped != null) {
                result.add(mapped);
            }
        }
        return result;
    }

    @Transactional
    public void cleanupDbCart(String email) {
        if (!isLoggedIn(email)) {
            return;
        }
        cartItemRepository.deleteUnavailableItemsByUserEmail(email);
        cartItemRepository.clampQuantitiesToAvailableStockByUserEmail(email);
        normalizeUserCart(email);
    }

    @Transactional
    public AddItemResult addItem(String email, @Nullable HttpSession session, long productId) {
        return addItem(email, session, productId, null, 1);
    }

    @Transactional
    public AddItemResult addItem(String email, @Nullable HttpSession session, long productId, int requestedQuantity) {
        return addItem(email, session, productId, null, requestedQuantity);
    }

    @Transactional
    public AddItemResult addItem(String email,
            @Nullable HttpSession session,
            long productId,
            @Nullable Long variantId,
            int requestedQuantity) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return AddItemResult.UNAVAILABLE;
        }

        LineContext line = resolveLine(product, variantId);
        if (!line.available()) {
            return AddItemResult.UNAVAILABLE;
        }

        int quantityToAdd = Math.max(1, requestedQuantity);
        if (isLoggedIn(email)) {
            Optional<CartItemEntity> existing = line.variantId() != null
                    ? cartItemRepository.findByUserEmailAndVariantId(email, line.variantId())
                    : cartItemRepository.findByUserEmailAndProductId(email, productId);
            if (existing.isPresent()) {
                CartItemEntity entity = existing.get();
                int currentQuantity = Math.max(0, entity.getQuantity());
                if (currentQuantity >= line.stock()) {
                    return AddItemResult.LIMIT_REACHED;
                }
                int targetQuantity = Math.min(line.stock(), currentQuantity + quantityToAdd);
                entity.setQuantity(targetQuantity);
                cartItemRepository.save(entity);
                return AddItemResult.ADDED;
            }

            int initialQuantity = Math.min(line.stock(), quantityToAdd);
            cartItemRepository.save(new CartItemEntity(email, productId, line.variantId(), initialQuantity));
            return AddItemResult.ADDED;
        }

        List<CartItem> cart = getSessionCart(session);
        CartItem found = cart.stream()
                .filter(item -> lineMatches(item, line.variantId(), productId))
                .findFirst()
                .orElse(null);
        if (found != null) {
            int currentQuantity = Math.max(0, found.getQuantity());
            if (currentQuantity >= line.stock()) {
                return AddItemResult.LIMIT_REACHED;
            }
            int targetQuantity = Math.min(line.stock(), currentQuantity + quantityToAdd);
            found.setQuantity(targetQuantity);
            return AddItemResult.ADDED;
        }

        int initialQuantity = Math.min(line.stock(), quantityToAdd);
        cart.add(new CartItem(
                productId,
                line.variantId(),
                line.variant() != null ? line.variant().getSku() : null,
                line.label(),
                product.getName(),
                line.price(),
                initialQuantity,
                line.imageUrl(),
                line.stock()));
        return AddItemResult.ADDED;
    }

    @Transactional
    public void increaseItem(String email, @Nullable HttpSession session, long lineId) {
        if (isLoggedIn(email)) {
            if (productVariantRepository == null) {
                Product product = productRepository.findById(lineId).orElse(null);
                if (product == null || Optional.ofNullable(product.getStock()).orElse(0) <= 0) {
                    return;
                }
                cartItemRepository.findByUserEmailAndProductId(email, lineId).ifPresent(entity -> {
                    if (entity.getQuantity() < Optional.ofNullable(product.getStock()).orElse(0)) {
                        entity.setQuantity(entity.getQuantity() + 1);
                        cartItemRepository.save(entity);
                    }
                });
                return;
            }
            cartItemRepository.findByUserEmailAndVariantId(email, lineId).ifPresentOrElse(entity -> {
                LineContext line = resolveLineByEntity(entity);
                if (line.available() && entity.getQuantity() < line.stock()) {
                    entity.setQuantity(entity.getQuantity() + 1);
                    cartItemRepository.save(entity);
                }
            }, () -> cartItemRepository.findByUserEmailAndProductId(email, lineId).ifPresent(entity -> {
                LineContext line = resolveLineByEntity(entity);
                if (line.available() && entity.getQuantity() < line.stock()) {
                    entity.setQuantity(entity.getQuantity() + 1);
                    cartItemRepository.save(entity);
                }
            }));
            return;
        }

        getSessionCart(session).stream()
                .filter(item -> lineMatches(item, lineId, lineId))
                .findFirst()
                .ifPresent(item -> {
                    LineContext line = resolveLineByCartItem(item);
                    if (line.available() && item.getQuantity() < line.stock()) {
                        item.setQuantity(item.getQuantity() + 1);
                    }
                });
    }

    @Transactional
    public void decreaseItem(String email, @Nullable HttpSession session, long lineId) {
        if (isLoggedIn(email)) {
            cartItemRepository.findByUserEmailAndVariantId(email, lineId).ifPresentOrElse(entity -> {
                if (entity.getQuantity() > 1) {
                    entity.setQuantity(entity.getQuantity() - 1);
                    cartItemRepository.save(entity);
                } else {
                    cartItemRepository.delete(entity);
                }
            }, () -> cartItemRepository.findByUserEmailAndProductId(email, lineId).ifPresent(entity -> {
                if (entity.getQuantity() > 1) {
                    entity.setQuantity(entity.getQuantity() - 1);
                    cartItemRepository.save(entity);
                } else {
                    cartItemRepository.delete(entity);
                }
            }));
            return;
        }

        List<CartItem> cart = getSessionCart(session);
        CartItem found = cart.stream()
                .filter(item -> lineMatches(item, lineId, lineId))
                .findFirst()
                .orElse(null);
        if (found != null) {
            if (found.getQuantity() > 1) {
                found.setQuantity(found.getQuantity() - 1);
            } else {
                cart.remove(found);
            }
        }
    }

    @Transactional
    public void removeItem(String email, @Nullable HttpSession session, long lineId) {
        if (isLoggedIn(email)) {
            cartItemRepository.deleteByUserEmailAndVariantId(email, lineId);
            cartItemRepository.deleteByUserEmailAndProductId(email, lineId);
            return;
        }
        getSessionCart(session).removeIf(item -> lineMatches(item, lineId, lineId));
    }

    @Transactional
    public void clearCart(String email, @Nullable HttpSession session) {
        if (isLoggedIn(email)) {
            cartItemRepository.deleteByUserEmail(email);
        } else if (session != null) {
            session.removeAttribute("cart");
        }
        if (session != null) {
            session.setAttribute("cartCount", 0);
        }
    }

    public List<CartItem> getCart(String email, @Nullable HttpSession session) {
        if (isLoggedIn(email)) {
            return getDbCartSnapshot(email);
        }
        return getSessionCart(session);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getUserCart(String email) {
        if (email == null || email.isBlank() || !isLoggedIn(email)) {
            return List.of();
        }
        return getDbCartSnapshot(email);
    }

    @Transactional(readOnly = true)
    public int countUserCartItems(String email) {
        if (email == null || email.isBlank() || !isLoggedIn(email)) {
            return 0;
        }
        return Math.toIntExact(cartItemRepository.sumQuantityByUserEmail(email));
    }

    @Scheduled(fixedDelayString = "${app.cart.cleanup-delay-ms:300000}")
    @Transactional
    public void cleanupDbCartForAllUsers() {
        cartItemRepository.deleteUnavailableItems();
        cartItemRepository.clampQuantitiesToAvailableStock();
        List<String> userEmails = cartItemRepository.findDistinctUserEmails();
        if (userEmails == null || userEmails.isEmpty()) {
            return;
        }
        for (String userEmail : userEmails) {
            normalizeUserCart(userEmail);
        }
    }

    public double calculateTotal(List<CartItem> cart) {
        return cart.stream()
                .mapToDouble(item -> Optional.ofNullable(item.getPrice()).orElse(0.0) * item.getQuantity())
                .sum();
    }

    private void normalizeUserCart(String email) {
        List<CartItemEntity> items = cartItemRepository.findByUserEmail(email);
        if (items == null || items.isEmpty()) {
            return;
        }

        List<Long> productIds = items.stream()
                .map(CartItemEntity::getProductId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds)
                .stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product));

        for (CartItemEntity entity : items) {
            Product product = productMap.get(entity.getProductId());
            LineContext line = product != null
                    ? resolveLine(product, entity.getVariantId())
                    : LineContext.unavailable();
            if (!line.available()) {
                cartItemRepository.delete(entity);
                continue;
            }
            int safeQty = Math.max(1, Math.min(entity.getQuantity(), line.stock()));
            if (safeQty != entity.getQuantity()) {
                entity.setQuantity(safeQty);
                cartItemRepository.save(entity);
            }
        }
    }

    private CartItem toCartItem(CartItemEntity entity, Product product, ProductVariant variant, String imageUrl) {
        if (entity == null || product == null) {
            return null;
        }
        int stock = productCommerceService.resolveEffectiveStock(product, variant);
        if (stock <= 0) {
            return null;
        }

        int quantity = Math.max(1, Math.min(entity.getQuantity(), stock));
        double price = productCommerceService.resolveEffectivePrice(product, variant);
        String resolvedImage = imageUrl != null && !imageUrl.isBlank() ? imageUrl : product.getImageUrl();

        return new CartItem(
                product.getId(),
                variant != null ? variant.getId() : entity.getVariantId(),
                variant != null ? variant.getSku() : null,
                variant != null ? variant.label() : null,
                product.getName(),
                price,
                quantity,
                resolvedImage,
                stock);
    }

    private Map<Long, String> buildPrimaryImageMap(List<Long> productIds) {
        if (productIds == null || productIds.isEmpty() || productImageRepository == null) {
            return Map.of();
        }
        Map<Long, String> map = new LinkedHashMap<>();
        for (ProductImage image : productImageRepository.findByProductIdsOrdered(productIds)) {
            if (image.getProduct() == null || image.getProduct().getId() == null) {
                continue;
            }
            map.putIfAbsent(image.getProduct().getId(), image.getUrl());
        }
        return map;
    }

    private boolean lineMatches(CartItem item, Long variantOrLineId, Long productIdFallback) {
        if (item == null) {
            return false;
        }
        if (item.getVariantId() != null && Objects.equals(item.getVariantId(), variantOrLineId)) {
            return true;
        }
        return item.getId() != null && Objects.equals(item.getId(), productIdFallback);
    }

    private ProductVariant resolveVariant(Product product,
            Long variantId,
            Map<Long, ProductVariant> variantMap) {
        if (variantId != null) {
            ProductVariant variant = variantMap.get(variantId);
            if (variant != null) {
                return variant;
            }
        }
        if (product == null) {
            return null;
        }
        return productCommerceService.resolveVariantOrDefault(product, variantId);
    }

    private LineContext resolveLineByEntity(CartItemEntity entity) {
        Long productId = entity.getProductId();
        if (productId == null) {
            return LineContext.unavailable();
        }
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return LineContext.unavailable();
        }
        return resolveLine(product, entity.getVariantId());
    }

    private LineContext resolveLineByCartItem(CartItem item) {
        if (item == null || item.getId() == null) {
            return LineContext.unavailable();
        }
        long productId = item.getId();
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            return LineContext.unavailable();
        }
        return resolveLine(product, item.getVariantId());
    }

    private LineContext resolveLine(Product product, @Nullable Long variantId) {
        ProductVariant variant = productCommerceService.resolveVariantOrDefault(product, variantId);
        if (variant == null || variant.getId() == null) {
            int productStock = Math.max(0, Optional.ofNullable(product.getStock()).orElse(0));
            if (productStock <= 0) {
                return LineContext.unavailable();
            }
            String image = product.getImageUrl();
            double price = product.getEffectivePrice() != null ? product.getEffectivePrice() : 0.0;
            return new LineContext(product, null, null, null, productStock, price, image, true);
        }

        int stock = Math.max(0, Optional.ofNullable(variant.getStock()).orElse(0));
        if (stock <= 0) {
            return LineContext.unavailable();
        }

        String image = productCommerceService.resolvePrimaryImageUrl(product, productCommerceService.loadImages(product.getId()));
        double price = productCommerceService.resolveEffectivePrice(product, variant);
        return new LineContext(
                product,
                variant,
                variant.getId(),
                variant.label(),
                stock,
                price,
                image,
                true);
    }

    private boolean isLoggedIn(String email) {
        return email != null && !email.equals("anonymousUser");
    }

    private String buildLineKey(Long variantId, Long productId) {
        if (variantId != null) {
            return "v:" + variantId;
        }
        return "p:" + (productId != null ? productId : -1L);
    }

    private record LineContext(
            Product product,
            ProductVariant variant,
            Long variantId,
            String label,
            int stock,
            double price,
            String imageUrl,
            boolean available) {

        static LineContext unavailable() {
            return new LineContext(null, null, null, null, 0, 0.0, null, false);
        }
    }
}
