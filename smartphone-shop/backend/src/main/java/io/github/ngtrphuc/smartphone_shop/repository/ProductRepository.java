package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%'))
          AND (:priceMin IS NULL OR p.price >= :priceMin)
          AND (:priceMax IS NULL OR p.price <= :priceMax)
        """)
    Page<Product> findWithFilters(
            @Param("keyword") String keyword,
            @Param("priceMin") Double priceMin,
            @Param("priceMax") Double priceMax,
            Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE (:keyword IS NULL OR LOWER(p.name) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%'))
          AND (:priceMin IS NULL OR p.price >= :priceMin)
          AND (:priceMax IS NULL OR p.price <= :priceMax)
        """)
    List<Product> findAllWithFilters(
            @Param("keyword") String keyword,
            @Param("priceMin") Double priceMin,
            @Param("priceMax") Double priceMax);

    @Query("""
        SELECT p FROM Product p
        WHERE (
                :keyword IS NULL
                OR (:keywordId IS NOT NULL AND p.id = :keywordId)
                OR LOWER(COALESCE(p.name, '')) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%')
                OR LOWER(COALESCE(p.description, '')) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%')
                OR LOWER(COALESCE(p.os, '')) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%')
                OR LOWER(COALESCE(p.chipset, '')) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%')
                OR LOWER(COALESCE(p.storage, '')) LIKE CONCAT('%', LOWER(CAST(:keyword as string)), '%')
              )
          AND (:minStock IS NULL OR p.stock >= :minStock)
          AND (:maxStock IS NULL OR p.stock <= :maxStock)
          AND (
                :brand IS NULL
                OR (LOWER(CAST(:brand as string)) = 'apple'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%'))
                OR (LOWER(CAST(:brand as string)) = 'samsung'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'samsung%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%'))
                OR (LOWER(CAST(:brand as string)) = 'google'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'google%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%'))
                OR (LOWER(CAST(:brand as string)) = 'oppo'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'oppo%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'find %'))
                OR (LOWER(CAST(:brand as string)) = 'vivo'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'vivo%')
                OR (LOWER(CAST(:brand as string)) = 'xiaomi'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%')
                OR (LOWER(CAST(:brand as string)) = 'sony'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'sony%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%'))
                OR (LOWER(CAST(:brand as string)) = 'asus'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'asus%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'rog%'))
                OR (LOWER(CAST(:brand as string)) = 'zte'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'zte%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%'))
                OR (LOWER(CAST(:brand as string)) = 'huawei'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'huawei%')
                OR (LOWER(CAST(:brand as string)) = 'honor'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'honor%')
                OR (LOWER(CAST(:brand as string)) = 'other'
                    AND NOT (
                        LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'samsung%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'google%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'oppo%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'find %'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'vivo%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'sony%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'asus%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'rog%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'zte%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'huawei%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'honor%'
                    ))
                OR LOWER(COALESCE(p.name, '')) LIKE CONCAT(LOWER(CAST(:brand as string)), '%')
          )
        """)
    Page<Product> findAdminProducts(
            @Param("keyword") String keyword,
            @Param("keywordId") Long keywordId,
            @Param("minStock") Integer minStock,
            @Param("maxStock") Integer maxStock,
            @Param("brand") String brand,
            Pageable pageable);

    @Query("SELECT p.name FROM Product p ORDER BY LOWER(p.name), p.id")
    List<String> findAllNamesOrdered();

    List<Product> findAllByOrderByNameAsc();

    List<Product> findByIdNotInOrderByNameAsc(List<Long> ids);

    List<Product> findByNameContainingIgnoreCase(String keyword);

    Optional<Product> findFirstByNameIgnoreCase(String name);

    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdIn(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id IN :ids")
    List<Product> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Query("""
        SELECT oi2.productId
        FROM OrderItem oi1
        JOIN OrderItem oi2 ON oi1.order.id = oi2.order.id
        JOIN oi1.order o
        WHERE oi1.productId = :productId
          AND oi2.productId IS NOT NULL
          AND oi2.productId <> :productId
          AND LOWER(COALESCE(o.status, '')) <> 'cancelled'
        GROUP BY oi2.productId
        ORDER BY COUNT(oi2.id) DESC, MIN(oi2.productId) ASC
        """)
    List<Long> findRecommendedProductIdsByCoPurchase(
            @Param("productId") Long productId,
            Pageable pageable);

    @Query("""
        SELECT p FROM Product p
        WHERE p.id <> :excludeId
        ORDER BY ABS(COALESCE(p.price, 0) - :targetPrice), LOWER(COALESCE(p.name, '')), p.id
        """)
    List<Product> findRecommendedProducts(
            @Param("excludeId") Long excludeId,
            @Param("targetPrice") Double targetPrice,
            Pageable pageable);
}
