package io.github.ngtrphuc.smartphone_shop.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;

import io.github.ngtrphuc.smartphone_shop.model.CartItem;
import io.github.ngtrphuc.smartphone_shop.model.CartItemEntity;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CartItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CartService {

    public enum AddItemResult {
        ADDED,
        LIMIT_REACHED,
        UNAVAILABLE
    }

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    public CartService(CartItemRepository cartItemRepository, ProductRepository productRepository) {
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
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

        List<Long> productIds = sessionCart.stream()
                .map(CartItem::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (productIds.isEmpty()) {
            if (session != null) {
                session.removeAttribute("cart");
            }
            syncCartCount(session, email);
            return;
        }

        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds)
                .stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p));

        Map<Long, CartItemEntity> existingByProductId = new LinkedHashMap<>();
        for (CartItemEntity existing : cartItemRepository.findByUserEmail(email)) {
            if (existing.getProductId() != null) {
                existingByProductId.putIfAbsent(existing.getProductId(), existing);
            }
        }

        for (CartItem item : sessionCart) {
            Long itemId = item.getId();
            if (itemId == null) {
                continue;
            }
            Product product = productMap.get(itemId);
            int maxStock = stockOf(product);
            if (maxStock <= 0) {
                continue;
            }

            int requestedQty = Math.max(item.getQuantity(), 0);
            if (requestedQty == 0) {
                continue;
            }

            CartItemEntity existing = existingByProductId.get(itemId);
            if (existing != null) {
                int mergedQty = Math.min(existing.getQuantity() + requestedQty, maxStock);
                if (mergedQty != existing.getQuantity()) {
                    existing.setQuantity(mergedQty);
                    cartItemRepository.save(existing);
                }
            } else {
                int initialQty = Math.min(requestedQty, maxStock);
                if (initialQty > 0) {
                    CartItemEntity created = new CartItemEntity(email, itemId, initialQty);
                    cartItemRepository.save(created);
                    existingByProductId.put(itemId, created);
                }
            }
        }
        if (session != null) {
            session.removeAttribute("cart");
        }
        syncCartCount(session, email);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getDbCart(String email) {
        return getDbCartSnapshot(email);
    }

    @Transactional(readOnly = true)
    public List<CartItem> getDbCartSnapshot(String email) {
        List<CartItemEntity> entities = cartItemRepository.findByUserEmail(email);
        if (entities.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = entities.stream().map(CartItemEntity::getProductId).toList();
        Map<Long, Product> productMap = productRepository.findAllByIdIn(productIds)
                .stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(Product::getId, p -> p));

        return entities.stream()
                .map(e -> toCartItem(e, productMap.get(e.getProductId())))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cleanupDbCart(String email) {
        if (!isLoggedIn(email)) {
            return;
        }
        cartItemRepository.deleteUnavailableItemsByUserEmail(email);
        cartItemRepository.clampQuantitiesToAvailableStockByUserEmail(email);
    }

    @Transactional
    public AddItemResult addItem(String email, @Nullable HttpSession session, long productId) {
        return addItem(email, session, productId, 1);
    }

    @Transactional
    public AddItemResult addItem(String email, @Nullable HttpSession session, long productId, int requestedQuantity) {
        Product p = productRepository.findById(productId).orElse(null);
        if (p == null) {
            return AddItemResult.UNAVAILABLE;
        }
        int maxStock = stockOf(p);
        if (maxStock <= 0) {
            return AddItemResult.UNAVAILABLE;
        }
        int quantityToAdd = Math.max(1, requestedQuantity);

        if (isLoggedIn(email)) {
            Optional<CartItemEntity> existing
                    = cartItemRepository.findByUserEmailAndProductId(email, productId);
            if (existing.isPresent()) {
                CartItemEntity e = existing.get();
                int currentQuantity = Math.max(0, e.getQuantity());
                if (currentQuantity >= maxStock) {
                    return AddItemResult.LIMIT_REACHED;
                }
                int targetQuantity = Math.min(maxStock, currentQuantity + quantityToAdd);
                e.setQuantity(targetQuantity);
                cartItemRepository.save(e);
                return AddItemResult.ADDED;
            } else {
                int initialQuantity = Math.min(maxStock, quantityToAdd);
                cartItemRepository.save(new CartItemEntity(email, productId, initialQuantity));
                return AddItemResult.ADDED;
            }
        } else {
            List<CartItem> cart = getSessionCart(session);
            CartItem found = cart.stream()
                    .filter(i -> i.getId() != null && i.getId() == productId)
                    .findFirst().orElse(null);
            if (found != null) {
                int currentQuantity = Math.max(0, found.getQuantity());
                if (currentQuantity >= maxStock) {
                    return AddItemResult.LIMIT_REACHED;
                }
                int targetQuantity = Math.min(maxStock, currentQuantity + quantityToAdd);
                found.setQuantity(targetQuantity);
                return AddItemResult.ADDED;
            } else {
                int initialQuantity = Math.min(maxStock, quantityToAdd);
                cart.add(new CartItem(
                        productId,
                        p.getName(),
                        p.getPrice(),
                        initialQuantity,
                        p.getImageUrl(),
                        maxStock));
                return AddItemResult.ADDED;
            }
        }
    }

    @Transactional
    public void increaseItem(String email, @Nullable HttpSession session, long productId) {
        Product p = productRepository.findById(productId).orElse(null);
        int maxStock = stockOf(p);
        if (maxStock <= 0) {
            return;
        }
        if (isLoggedIn(email)) {
            cartItemRepository.findByUserEmailAndProductId(email, productId).ifPresent(e -> {
                if (e.getQuantity() < maxStock) {
                    e.setQuantity(e.getQuantity() + 1);
                    cartItemRepository.save(e);
                }
            });
        } else {
            getSessionCart(session).stream()
                    .filter(i -> i.getId() != null && i.getId() == productId)
                    .findFirst()
                    .ifPresent(i -> {
                        if (i.getQuantity() < maxStock) {
                            i.setQuantity(i.getQuantity() + 1);
                        }
                    });
        }
    }

    @Transactional
    public void decreaseItem(String email, @Nullable HttpSession session, long productId) {
        if (isLoggedIn(email)) {
            cartItemRepository.findByUserEmailAndProductId(email, productId).ifPresent(e -> {
                if (e.getQuantity() > 1) {
                    e.setQuantity(e.getQuantity() - 1);
                    cartItemRepository.save(e);
                } else {
                    cartItemRepository.delete(e);
                }
            });
        } else {
            List<CartItem> cart = getSessionCart(session);
            CartItem found = cart.stream()
                    .filter(i -> i.getId() != null && i.getId() == productId)
                    .findFirst().orElse(null);
            if (found != null) {
                if (found.getQuantity() > 1) {
                    found.setQuantity(found.getQuantity() - 1); 
                }else {
                    cart.remove(found);
                }
            }
        }
    }

    @Transactional
    public void removeItem(String email, @Nullable HttpSession session, long productId) {
        if (isLoggedIn(email)) {
            cartItemRepository.deleteByUserEmailAndProductId(email, productId);
        } else {
            getSessionCart(session).removeIf(i -> i.getId() != null && i.getId() == productId);
        }
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
    }

    public double calculateTotal(List<CartItem> cart) {
        return cart.stream()
                .mapToDouble(i -> Optional.ofNullable(i.getPrice()).orElse(0.0) * i.getQuantity())
                .sum();
    }

    private CartItem toCartItem(CartItemEntity entity, Product product) {
        int stock = stockOf(product);
        if (entity == null || product == null || stock <= 0) {
            return null;
        }
        int quantity = Math.max(1, Math.min(entity.getQuantity(), stock));
        return new CartItem(
                entity.getProductId(),
                product.getName(),
                product.getPrice(),
                quantity,
                product.getImageUrl(),
                stock);
    }

    private int stockOf(Product product) {
        return Optional.ofNullable(product)
                .map(Product::getStock)
                .orElse(0);
    }

    private boolean isLoggedIn(String email) {
        return email != null && !email.equals("anonymousUser");
    }
}
