package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.ProductVariant;
import jakarta.persistence.LockModeType;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId ORDER BY pv.active DESC, pv.id ASC")
    List<ProductVariant> findByProductIdOrderByActiveDescIdAsc(@Param("productId") Long productId);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id IN :productIds AND pv.active = true ORDER BY pv.id ASC")
    List<ProductVariant> findActiveByProductIds(@Param("productIds") Collection<Long> productIds);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id IN :ids")
    List<ProductVariant> findAllByIdIn(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id IN :ids")
    List<ProductVariant> findAllByIdInForUpdate(@Param("ids") Collection<Long> ids);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.stock = pv.stock - :quantity
        WHERE pv.id = :id
          AND pv.stock >= :quantity
        """)
    int decrementStockIfAvailable(@Param("id") Long id, @Param("quantity") int quantity);

    @Modifying
    @Query("""
        UPDATE ProductVariant pv
        SET pv.stock = pv.stock + :quantity
        WHERE pv.id = :id
        """)
    int incrementStock(@Param("id") Long id, @Param("quantity") int quantity);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.id = :id AND pv.active = true")
    Optional<ProductVariant> findActiveById(@Param("id") Long id);

    @Query("SELECT pv FROM ProductVariant pv WHERE pv.product.id = :productId AND pv.active = true ORDER BY pv.id ASC")
    List<ProductVariant> findActiveByProductId(@Param("productId") Long productId);

    boolean existsBySkuIgnoreCase(String sku);

    void deleteByProduct_Id(Long productId);
}
