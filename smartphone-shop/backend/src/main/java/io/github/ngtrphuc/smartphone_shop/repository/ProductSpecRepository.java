package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.ProductSpec;

public interface ProductSpecRepository extends JpaRepository<ProductSpec, Long> {

    @Query("SELECT ps FROM ProductSpec ps WHERE ps.product.id = :productId ORDER BY ps.sortOrder ASC, ps.id ASC")
    List<ProductSpec> findByProductIdOrdered(@Param("productId") Long productId);

    void deleteByProduct_Id(Long productId);
}
