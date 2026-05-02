package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ngtrphuc.smartphone_shop.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findBySlugIgnoreCase(String slug);
}
