package io.github.ngtrphuc.smartphone_shop.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.CartItemEntity;
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    List<CartItemEntity> findByUserEmail(String userEmail);
    Optional<CartItemEntity> findByUserEmailAndProductId(String userEmail, Long productId);
    @Query("SELECT DISTINCT c.userEmail FROM CartItemEntity c")
    List<String> findDistinctUserEmails();

    @Query("SELECT COALESCE(SUM(c.quantity), 0) FROM CartItemEntity c WHERE c.userEmail = :email")
    long sumQuantityByUserEmail(@Param("email") String email);

    void deleteByUserEmail(String userEmail);
    void deleteByUserEmailAndProductId(String userEmail, Long productId);
    void deleteByProductId(Long productId);

    @Modifying
    @Query("UPDATE CartItemEntity c SET c.quantity = :qty WHERE c.userEmail = :email AND c.productId = :productId")
    void updateQuantity(@Param("email") String email,
                        @Param("productId") Long productId,
                        @Param("qty") int qty);

    @Modifying
    @Query("""
            DELETE FROM CartItemEntity c
            WHERE c.userEmail = :email
              AND NOT EXISTS (
                    SELECT 1
                    FROM Product p
                    WHERE p.id = c.productId
                      AND COALESCE(p.stock, 0) > 0
              )
            """)
    int deleteUnavailableItemsByUserEmail(@Param("email") String email);

    @Modifying
    @Query("""
            UPDATE CartItemEntity c
            SET c.quantity = (
                SELECT CASE
                    WHEN c.quantity < 1 THEN 1
                    WHEN c.quantity > p.stock THEN p.stock
                    ELSE c.quantity
                END
                FROM Product p
                WHERE p.id = c.productId
            )
            WHERE c.userEmail = :email
              AND EXISTS (
                    SELECT 1
                    FROM Product p
                    WHERE p.id = c.productId
                      AND COALESCE(p.stock, 0) > 0
                      AND (c.quantity < 1 OR c.quantity > p.stock)
              )
            """)
    int clampQuantitiesToAvailableStockByUserEmail(@Param("email") String email);

    @Modifying
    @Query("""
            DELETE FROM CartItemEntity c
            WHERE NOT EXISTS (
                    SELECT 1
                    FROM Product p
                    WHERE p.id = c.productId
                      AND COALESCE(p.stock, 0) > 0
            )
            """)
    int deleteUnavailableItems();

    @Modifying
    @Query("""
            UPDATE CartItemEntity c
            SET c.quantity = (
                SELECT CASE
                    WHEN c.quantity < 1 THEN 1
                    WHEN c.quantity > p.stock THEN p.stock
                    ELSE c.quantity
                END
                FROM Product p
                WHERE p.id = c.productId
            )
            WHERE EXISTS (
                    SELECT 1
                    FROM Product p
                    WHERE p.id = c.productId
                      AND COALESCE(p.stock, 0) > 0
                      AND (c.quantity < 1 OR c.quantity > p.stock)
            )
            """)
    int clampQuantitiesToAvailableStock();
}
