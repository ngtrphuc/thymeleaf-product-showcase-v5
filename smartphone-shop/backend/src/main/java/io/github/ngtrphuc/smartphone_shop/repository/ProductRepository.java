package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.Product;
import jakarta.persistence.LockModeType;

public interface ProductRepository extends JpaRepository<Product, Long> {

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

    @Query(value = """
        SELECT p.* FROM products p
        WHERE (:keyword IS NULL OR LOWER(COALESCE(p.name, '')) LIKE CONCAT('%', LOWER(CAST(:keyword AS text)), '%'))
          AND (:priceMin IS NULL OR p.price >= :priceMin)
          AND (:priceMax IS NULL OR p.price <= :priceMax)
          AND (
                :brand IS NULL
                OR :brand = ''
                OR (LOWER(CAST(:brand AS text)) = 'apple'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%%'))
                OR (LOWER(CAST(:brand AS text)) = 'samsung'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'samsung%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%%'))
                OR (LOWER(CAST(:brand AS text)) = 'google'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'google%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%%'))
                OR (LOWER(CAST(:brand AS text)) = 'oppo'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'oppo%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'find %%'))
                OR (LOWER(CAST(:brand AS text)) = 'vivo'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'vivo%%')
                OR (LOWER(CAST(:brand AS text)) = 'xiaomi'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%%')
                OR (LOWER(CAST(:brand AS text)) = 'sony'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'sony%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%%'))
                OR (LOWER(CAST(:brand AS text)) = 'asus'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'asus%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'rog%%'))
                OR (LOWER(CAST(:brand AS text)) = 'zte'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'zte%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%%'))
                OR (LOWER(CAST(:brand AS text)) = 'huawei'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'huawei%%')
                OR (LOWER(CAST(:brand AS text)) = 'honor'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'honor%%')
                OR (LOWER(CAST(:brand AS text)) = 'other'
                    AND NOT (
                        LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'samsung%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'google%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'oppo%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'find %%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'vivo%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'sony%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'asus%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'rog%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'zte%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'huawei%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'honor%%'
                    ))
                OR LOWER(COALESCE(p.name, '')) LIKE CONCAT(LOWER(CAST(:brand AS text)), '%%')
          )
          AND (
                :batteryRange IS NULL
                OR :batteryRange = ''
                OR (:batteryRange = 'under5000'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                        0
                    ) < 5000)
                OR (:batteryRange = 'over5000'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                        0
                    ) >= 5000)
          )
          AND (:batteryMin IS NULL OR COALESCE(
                CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                0
              ) >= :batteryMin)
          AND (:batteryMax IS NULL OR COALESCE(
                CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                0
              ) <= :batteryMax)
          AND (
                :screenSize IS NULL
                OR :screenSize = ''
                OR (:screenSize = 'under6.5'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.size, ''), '[^0-9.]', '', 'g'), '') AS DOUBLE PRECISION),
                        0.0
                    ) < 6.5)
                OR (:screenSize = '6.5to6.8'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.size, ''), '[^0-9.]', '', 'g'), '') AS DOUBLE PRECISION),
                        0.0
                    ) BETWEEN 6.5 AND 6.8)
                OR (:screenSize = 'over6.8'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.size, ''), '[^0-9.]', '', 'g'), '') AS DOUBLE PRECISION),
                        0.0
                    ) > 6.8)
          )
        ORDER BY
          CASE WHEN :sort = 'name_desc' THEN LOWER(COALESCE(p.name, '')) END DESC,
          CASE WHEN :sort = 'price_asc' THEN p.price END ASC,
          CASE WHEN :sort = 'price_desc' THEN p.price END DESC,
          CASE WHEN :sort = 'name_asc' THEN LOWER(COALESCE(p.name, '')) END ASC,
          p.id ASC
        """,
            countQuery = """
        SELECT COUNT(*) FROM products p
        WHERE (:keyword IS NULL OR LOWER(COALESCE(p.name, '')) LIKE CONCAT('%', LOWER(CAST(:keyword AS text)), '%'))
          AND (:priceMin IS NULL OR p.price >= :priceMin)
          AND (:priceMax IS NULL OR p.price <= :priceMax)
          AND (
                :brand IS NULL
                OR :brand = ''
                OR (LOWER(CAST(:brand AS text)) = 'apple'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%%'))
                OR (LOWER(CAST(:brand AS text)) = 'samsung'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'samsung%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%%'))
                OR (LOWER(CAST(:brand AS text)) = 'google'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'google%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%%'))
                OR (LOWER(CAST(:brand AS text)) = 'oppo'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'oppo%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'find %%'))
                OR (LOWER(CAST(:brand AS text)) = 'vivo'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'vivo%%')
                OR (LOWER(CAST(:brand AS text)) = 'xiaomi'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%%')
                OR (LOWER(CAST(:brand AS text)) = 'sony'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'sony%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%%'))
                OR (LOWER(CAST(:brand AS text)) = 'asus'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'asus%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'rog%%'))
                OR (LOWER(CAST(:brand AS text)) = 'zte'
                    AND (LOWER(COALESCE(p.name, '')) LIKE 'zte%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%%'
                         OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%%'))
                OR (LOWER(CAST(:brand AS text)) = 'huawei'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'huawei%%')
                OR (LOWER(CAST(:brand AS text)) = 'honor'
                    AND LOWER(COALESCE(p.name, '')) LIKE 'honor%%')
                OR (LOWER(CAST(:brand AS text)) = 'other'
                    AND NOT (
                        LOWER(COALESCE(p.name, '')) LIKE 'apple iphone%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'iphone%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'samsung%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'galaxy%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'google%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'pixel%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'oppo%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'find %%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'vivo%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'xiaomi%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'sony%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'xperia%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'asus%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'rog%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'zte%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'nubia%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'redmagic%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'huawei%%'
                        OR LOWER(COALESCE(p.name, '')) LIKE 'honor%%'
                    ))
                OR LOWER(COALESCE(p.name, '')) LIKE CONCAT(LOWER(CAST(:brand AS text)), '%%')
          )
          AND (
                :batteryRange IS NULL
                OR :batteryRange = ''
                OR (:batteryRange = 'under5000'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                        0
                    ) < 5000)
                OR (:batteryRange = 'over5000'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                        0
                    ) >= 5000)
          )
          AND (:batteryMin IS NULL OR COALESCE(
                CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                0
              ) >= :batteryMin)
          AND (:batteryMax IS NULL OR COALESCE(
                CAST(NULLIF(regexp_replace(COALESCE(p.battery, ''), '[^0-9]', '', 'g'), '') AS INTEGER),
                0
              ) <= :batteryMax)
          AND (
                :screenSize IS NULL
                OR :screenSize = ''
                OR (:screenSize = 'under6.5'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.size, ''), '[^0-9.]', '', 'g'), '') AS DOUBLE PRECISION),
                        0.0
                    ) < 6.5)
                OR (:screenSize = '6.5to6.8'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.size, ''), '[^0-9.]', '', 'g'), '') AS DOUBLE PRECISION),
                        0.0
                    ) BETWEEN 6.5 AND 6.8)
                OR (:screenSize = 'over6.8'
                    AND COALESCE(
                        CAST(NULLIF(regexp_replace(COALESCE(p.size, ''), '[^0-9.]', '', 'g'), '') AS DOUBLE PRECISION),
                        0.0
                    ) > 6.8)
          )
        """,
            nativeQuery = true)
    Page<Product> findCatalogPage(
            @Param("keyword") String keyword,
            @Param("priceMin") Double priceMin,
            @Param("priceMax") Double priceMax,
            @Param("brand") String brand,
            @Param("batteryRange") String batteryRange,
            @Param("batteryMin") Integer batteryMin,
            @Param("batteryMax") Integer batteryMax,
            @Param("screenSize") String screenSize,
            @Param("sort") String sort,
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
        """)
    Page<Product> findAdminProducts(
            @Param("keyword") String keyword,
            @Param("keywordId") Long keywordId,
            @Param("minStock") Integer minStock,
            @Param("maxStock") Integer maxStock,
            Pageable pageable);

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
        """)
    List<Product> findAllAdminProducts(
            @Param("keyword") String keyword,
            @Param("keywordId") Long keywordId,
            @Param("minStock") Integer minStock,
            @Param("maxStock") Integer maxStock);

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
        SELECT p FROM Product p
        WHERE p.id <> :excludeId
        ORDER BY ABS(COALESCE(p.price, 0) - :targetPrice), LOWER(COALESCE(p.name, '')), p.id
        """)
    List<Product> findRecommendedProducts(
            @Param("excludeId") Long excludeId,
            @Param("targetPrice") Double targetPrice,
            Pageable pageable);
}
