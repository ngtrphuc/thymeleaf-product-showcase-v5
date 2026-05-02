package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ngtrphuc.smartphone_shop.model.Brand;

public interface BrandRepository extends JpaRepository<Brand, Long> {
    Optional<Brand> findBySlugIgnoreCase(String slug);
}
