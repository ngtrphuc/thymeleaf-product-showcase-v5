package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.PaymentMethod;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findByUserEmailAndActiveTrueOrderByIsDefaultDescCreatedAtDesc(String userEmail);

    Optional<PaymentMethod> findByUserEmailAndIsDefaultTrueAndActiveTrue(String userEmail);

    @Modifying
    @Query("UPDATE PaymentMethod p SET p.isDefault = false WHERE p.userEmail = :email")
    void clearDefaultForUser(@Param("email") String email);

    @Query("SELECT COUNT(p) FROM PaymentMethod p WHERE p.userEmail = :email AND p.active = true")
    long countActiveByUser(@Param("email") String email);
}

