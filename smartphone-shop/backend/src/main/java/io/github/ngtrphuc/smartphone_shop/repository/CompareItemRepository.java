package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ngtrphuc.smartphone_shop.model.CompareItemEntity;

public interface CompareItemRepository extends JpaRepository<CompareItemEntity, Long> {

    List<CompareItemEntity> findByUserEmailOrderByCreatedAtDesc(String userEmail);

    boolean existsByUserEmailAndProductId(String userEmail, Long productId);

    long countByUserEmail(String userEmail);

    void deleteByUserEmailAndProductId(String userEmail, Long productId);

    void deleteByUserEmail(String userEmail);
}
