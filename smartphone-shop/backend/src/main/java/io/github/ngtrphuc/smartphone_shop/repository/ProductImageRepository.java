package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.ProductImage;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id = :productId ORDER BY pi.primary DESC, pi.sortOrder ASC, pi.id ASC")
    List<ProductImage> findByProductIdOrdered(@Param("productId") Long productId);

    @Query("SELECT pi FROM ProductImage pi WHERE pi.product.id IN :productIds ORDER BY pi.primary DESC, pi.sortOrder ASC, pi.id ASC")
    List<ProductImage> findByProductIdsOrdered(@Param("productIds") Collection<Long> productIds);

    void deleteByProduct_Id(Long productId);
}
