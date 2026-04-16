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
        WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
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
        WHERE (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
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
                OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.os, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.chipset, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.storage, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
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
                OR LOWER(COALESCE(p.name, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.os, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.chipset, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(p.storage, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
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
