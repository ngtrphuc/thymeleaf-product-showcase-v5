package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.WishlistItemEntity;

public interface WishlistItemRepository extends JpaRepository<WishlistItemEntity, Long> {

    List<WishlistItemEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    Optional<WishlistItemEntity> findByUserEmailAndProductId(String userEmail, Long productId);

    boolean existsByUserEmailAndProductId(String userEmail, Long productId);

    long countByUserEmail(String userEmail);

    void deleteByUserEmailAndProductId(String userEmail, Long productId);

    @Query("SELECT DISTINCT w.userEmail FROM WishlistItemEntity w")
    List<String> findDistinctUserEmails();

    @Query("SELECT DISTINCT w.userEmail FROM WishlistItemEntity w ORDER BY w.userEmail")
    List<String> findDistinctUserEmails(Pageable pageable);

    @Modifying
    @Query("""
            DELETE FROM WishlistItemEntity w
            WHERE w.userEmail = :email
              AND NOT EXISTS (
                    SELECT 1
                    FROM Product p
                    WHERE p.id = w.productId
              )
            """)
    int deleteOrphanedByUserEmail(@Param("email") String email);

    @Modifying
    @Query("""
            DELETE FROM WishlistItemEntity w
            WHERE NOT EXISTS (
                    SELECT 1
                    FROM Product p
                    WHERE p.id = w.productId
            )
            """)
    int deleteOrphanedItems();
}
