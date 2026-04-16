package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ngtrphuc.smartphone_shop.model.WishlistItemEntity;

public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {

    List<WishlistItemEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<WishlistItemEntity> findByUserEmailAndProductId(String userEmail, Long productId);

    boolean existsByUserEmailAndProductId(String userEmail, Long productId);

    long countByUserEmail(String userEmail);

    void deleteByUserEmailAndProductId(String userEmail, Long productId);
}
