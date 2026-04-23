package io.github.ngtrphuc.smartphone_shop.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.common.exception.ValidationException;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItem;
import io.github.ngtrphuc.smartphone_shop.model.WishlistItemEntity;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import io.github.ngtrphuc.smartphone_shop.repository.WishlistItemRepository;

@Service
public class WishlistService {

    public enum AddResult {
        ADDED,
        ALREADY_EXISTS,
        UNAVAILABLE
    }

    private static final int MAX_EMAIL_LENGTH = 100;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final AtomicInteger cleanupPage = new AtomicInteger(0);
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final int cleanupBatchSize;

    public WishlistService(WishlistItemRepository wishlistItemRepository,
            ProductRepository productRepository,
            @Value("${app.wishlist.cleanup-batch-size:200}") int cleanupBatchSize) {
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.cleanupBatchSize = Math.max(1, cleanupBatchSize);
    }

    @Transactional
    public AddResult addItem(String email, long productId) {
        String normalizedEmail = normalizeEmail(email);
        if (!productRepository.existsById(productId)) {
            return AddResult.UNAVAILABLE;
        }
        if (wishlistItemRepository.existsByUserEmailAndProductId(normalizedEmail, productId)) {
            return AddResult.ALREADY_EXISTS;
        }
        wishlistItemRepository.save(new WishlistItemEntity(normalizedEmail, productId));
        return AddResult.ADDED;
    }

    @Transactional
    public boolean removeItem(String email, long productId) {
        String normalizedEmail = normalizeEmail(email);
        WishlistItemEntity existing =
                wishlistItemRepository.findByUserEmailAndProductId(normalizedEmail, productId).orElse(null);
        if (existing == null) {
            return false;
        }
        wishlistItemRepository.delete(existing);
        return true;
    }

    @Transactional(readOnly = true)
    public List<WishlistItem> getWishlist(String email) {
        String normalizedEmail = normalizeEmail(email);
        List<WishlistItemEntity> entities = wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc(normalizedEmail);
        if (entities.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = entities.stream()
                .map(WishlistItemEntity::getProductId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Map<Long, Product> productsById = productRepository.findAllByIdIn(productIds).stream()
                .filter(product -> product.getId() != null)
                .collect(Collectors.toMap(Product::getId, product -> product));

        List<WishlistItem> result = new ArrayList<>();
        for (WishlistItemEntity entity : entities) {
            Product product = productsById.get(entity.getProductId());
            if (product == null) {
                continue;
            }
            result.add(new WishlistItem(
                    product.getId(),
                    product.getName(),
                    product.getPrice(),
                    product.getImageUrl(),
                    product.getStock(),
                    entity.getCreatedAt()));
        }
        return result;
    }

    @Transactional
    public void cleanupOrphanedItems(String email) {
        String normalizedEmail = normalizeEmail(email);
        List<WishlistItemEntity> entities = wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc(normalizedEmail);
        if (entities.isEmpty()) {
            return;
        }

        List<Long> productIds = entities.stream()
                .map(WishlistItemEntity::getProductId)
                .filter(id -> id != null)
                .distinct()
                .toList();
        Set<Long> existingProductIds = productRepository.findAllByIdIn(productIds).stream()
                .map(Product::getId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        List<WishlistItemEntity> orphaned = entities.stream()
                .filter(entity -> entity.getProductId() == null || !existingProductIds.contains(entity.getProductId()))
                .toList();
        if (!orphaned.isEmpty()) {
            wishlistItemRepository.deleteAll(orphaned);
        }
    }

    @Scheduled(fixedDelayString = "${app.wishlist.cleanup-delay-ms:300000}")
    @Transactional
    public void cleanupOrphanedItemsForAllUsers() {
        int currentPage = Math.max(0, cleanupPage.get());
        List<String> emails = wishlistItemRepository.findDistinctUserEmails(
                PageRequest.of(currentPage, cleanupBatchSize));
        if (emails.isEmpty()) {
            cleanupPage.set(0);
            emails = wishlistItemRepository.findDistinctUserEmails(PageRequest.of(0, cleanupBatchSize));
        }
        for (String email : emails) {
            cleanupOrphanedItems(email);
        }
        if (emails.size() < cleanupBatchSize) {
            cleanupPage.set(0);
        } else {
            cleanupPage.incrementAndGet();
        }
    }

    @Transactional(readOnly = true)
    public Set<Long> getWishlistedProductIds(String email) {
        String normalizedEmail = normalizeEmail(email);
        return wishlistItemRepository.findByUserEmailOrderByCreatedAtDesc(normalizedEmail)
                .stream()
                .map(WishlistItemEntity::getProductId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    @Transactional(readOnly = true)
    public boolean isWishlisted(String email, long productId) {
        String normalizedEmail = normalizeEmail(email);
        return wishlistItemRepository.existsByUserEmailAndProductId(normalizedEmail, productId);
    }

    @Transactional(readOnly = true)
    public long countWishlist(String email) {
        String normalizedEmail = normalizeEmail(email);
        return wishlistItemRepository.countByUserEmail(normalizedEmail);
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new ValidationException("User email cannot be empty.");
        }
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new ValidationException("User email is too long.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new ValidationException("User email is invalid.");
        }
        return normalized;
    }
}
