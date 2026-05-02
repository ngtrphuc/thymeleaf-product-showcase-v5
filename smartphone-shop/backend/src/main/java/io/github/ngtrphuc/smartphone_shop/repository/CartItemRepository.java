package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.CartItemEntity;

public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    List<CartItemEntity> findByUserEmail(String userEmail);

    Optional<CartItemEntity> findByUserEmailAndVariantId(String userEmail, Long variantId);

    Optional<CartItemEntity> findByUserEmailAndProductId(String userEmail, Long productId);

    @Query("SELECT DISTINCT c.userEmail FROM CartItemEntity c")
    List<String> findDistinctUserEmails();

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CartItemEntity c WHERE c.userEmail = :email")
    long sumQuantityByUserEmail(@Param("email") String email);

    void deleteByUserEmail(String userEmail);

    void deleteByUserEmailAndVariantId(String userEmail, Long variantId);

    void deleteByUserEmailAndProductId(String userEmail, Long productId);

    void deleteByProductId(Long productId);

    // Backward-compatible bulk cleanup hooks used by existing tests.
    default void deleteUnavailableItemsByUserEmail(String userEmail) {
        // No-op by default; CartService performs deterministic normalization.
    }

    default void clampQuantitiesToAvailableStockByUserEmail(String userEmail) {
        // No-op by default; CartService performs deterministic normalization.
    }

    default void deleteUnavailableItems() {
        // No-op by default; CartService performs deterministic normalization.
    }

    default void clampQuantitiesToAvailableStock() {
        // No-op by default; CartService performs deterministic normalization.
    }
}
