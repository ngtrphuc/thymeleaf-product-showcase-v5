package io.github.ngtrphuc.smartphone_shop.service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.lang.Nullable;
import org.springframework.transaction.annotation.Transactional;

import io.github.ngtrphuc.smartphone_shop.model.CompareItemEntity;
import io.github.ngtrphuc.smartphone_shop.model.Product;
import io.github.ngtrphuc.smartphone_shop.repository.CompareItemRepository;
import io.github.ngtrphuc.smartphone_shop.repository.ProductRepository;
import jakarta.servlet.http.HttpSession;

@Service
public class CompareService {

    public enum AddResult {
        ADDED,
        ALREADY_EXISTS,
        LIMIT_REACHED,
        UNAVAILABLE
    }

    private static final String SESSION_KEY = "compareIds";
    private static final int MAX_COMPARE = 3;
    private static final int MAX_EMAIL_LENGTH = 100;
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final CompareItemRepository compareItemRepository;
    private final ProductRepository productRepository;

    public CompareService(CompareItemRepository compareItemRepository, ProductRepository productRepository) {
        this.compareItemRepository = compareItemRepository;
        this.productRepository = productRepository;
    }

    @Transactional(readOnly = true)
    public List<Long> getCompareIds(String email, @Nullable HttpSession session) {
        if (isLoggedIn(email)) {
            String normalizedEmail = normalizeEmail(email);
            List<Long> ids = getDbCompareIds(normalizedEmail);
            if (session != null) {
                session.setAttribute(SESSION_KEY, new ArrayList<>(ids));
            }
            return ids;
        }
        return getSessionIds(session);
    }

    @Transactional
    public AddResult addItem(String email, @Nullable HttpSession session, long productId) {
        if (!productRepository.existsById(productId)) {
            return AddResult.UNAVAILABLE;
        }

        if (isLoggedIn(email)) {
            String normalizedEmail = normalizeEmail(email);
            if (compareItemRepository.existsByUserEmailAndProductId(normalizedEmail, productId)) {
                return AddResult.ALREADY_EXISTS;
            }
            if (compareItemRepository.countByUserEmail(normalizedEmail) >= MAX_COMPARE) {
                return AddResult.LIMIT_REACHED;
            }
            compareItemRepository.save(new CompareItemEntity(normalizedEmail, productId));
            syncSessionFromDb(session, normalizedEmail);
            return AddResult.ADDED;
        }

        List<Long> ids = getSessionIds(session);
        Long boxedId = productId;
        if (ids.contains(boxedId)) {
            return AddResult.ALREADY_EXISTS;
        }
        if (ids.size() >= MAX_COMPARE) {
            return AddResult.LIMIT_REACHED;
        }
        ids.add(boxedId);
        if (session != null) {
            session.setAttribute(SESSION_KEY, ids);
        }
        return AddResult.ADDED;
    }

    @Transactional
    public void removeItem(String email, @Nullable HttpSession session, long productId) {
        if (isLoggedIn(email)) {
            String normalizedEmail = normalizeEmail(email);
            compareItemRepository.deleteByUserEmailAndProductId(normalizedEmail, productId);
            syncSessionFromDb(session, normalizedEmail);
            return;
        }
        List<Long> ids = getSessionIds(session);
        ids.remove(Long.valueOf(productId));
        if (session != null) {
            session.setAttribute(SESSION_KEY, ids);
        }
    }

    @Transactional
    public void clear(String email, @Nullable HttpSession session) {
        if (isLoggedIn(email)) {
            compareItemRepository.deleteByUserEmail(normalizeEmail(email));
        }
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    @Transactional
    public void saveCompareIds(String email, @Nullable HttpSession session, List<Long> ids) {
        List<Long> sanitized = sanitizeIds(ids);
        if (isLoggedIn(email)) {
            String normalizedEmail = normalizeEmail(email);
            compareItemRepository.deleteByUserEmail(normalizedEmail);
            for (Long id : sanitized) {
                compareItemRepository.save(new CompareItemEntity(normalizedEmail, id));
            }
            syncSessionFromDb(session, normalizedEmail);
            return;
        }
        if (session != null) {
            session.setAttribute(SESSION_KEY, sanitized);
        }
    }

    @Transactional
    public void mergeSessionCompareToDb(@Nullable HttpSession session, String email) {
        if (!isLoggedIn(email)) {
            return;
        }
        String normalizedEmail = normalizeEmail(email);
        List<Long> sessionIds = getSessionIds(session);
        if (!sessionIds.isEmpty()) {
            LinkedHashSet<Long> mergedIds = new LinkedHashSet<>(getDbCompareIds(normalizedEmail));
            Set<Long> existingProductIds = resolveExistingProductIds(sessionIds);
            for (Long id : sessionIds) {
                if (id == null || mergedIds.size() >= MAX_COMPARE) {
                    break;
                }
                if (mergedIds.contains(id) || !existingProductIds.contains(id)) {
                    continue;
                }
                compareItemRepository.save(new CompareItemEntity(normalizedEmail, id));
                mergedIds.add(id);
            }
        }
        syncSessionFromDb(session, normalizedEmail);
    }

    @Transactional(readOnly = true)
    public int getMaxCompare() {
        return MAX_COMPARE;
    }

    private void syncSessionFromDb(@Nullable HttpSession session, String normalizedEmail) {
        if (session != null) {
            session.setAttribute(SESSION_KEY, new ArrayList<>(getDbCompareIds(normalizedEmail)));
        }
    }

    private List<Long> getDbCompareIds(String normalizedEmail) {
        return sanitizeIds(compareItemRepository.findByUserEmailOrderByCreatedAtDesc(normalizedEmail).stream()
                .map(CompareItemEntity::getProductId)
                .toList());
    }

    @SuppressWarnings("unchecked")
    private List<Long> getSessionIds(@Nullable HttpSession session) {
        if (session == null) {
            return new ArrayList<>();
        }
        Object obj = session.getAttribute(SESSION_KEY);
        if (obj instanceof List<?> rawList && rawList.stream().allMatch(Long.class::isInstance)) {
            return sanitizeIds((List<Long>) rawList);
        }
        return new ArrayList<>();
    }

    private List<Long> sanitizeIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        Set<Long> existingProductIds = resolveExistingProductIds(ids);
        LinkedHashSet<Long> unique = new LinkedHashSet<>();
        for (Long id : ids) {
            if (id == null || !existingProductIds.contains(id)) {
                continue;
            }
            unique.add(id);
            if (unique.size() >= MAX_COMPARE) {
                break;
            }
        }
        return new ArrayList<>(unique);
    }

    private Set<Long> resolveExistingProductIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        List<Long> candidateIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (candidateIds.isEmpty()) {
            return Set.of();
        }
        return productRepository.findAllByIdIn(candidateIds).stream()
                .map(Product::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    private boolean isLoggedIn(String email) {
        return email != null && !email.isBlank() && !"anonymousUser".equals(email);
    }

    private String normalizeEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
        if (normalized.isBlank()) {
            throw new IllegalArgumentException("User email cannot be empty.");
        }
        if (normalized.length() > MAX_EMAIL_LENGTH) {
            throw new IllegalArgumentException("User email is too long.");
        }
        if (!EMAIL_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException("User email is invalid.");
        }
        return normalized;
    }
}
