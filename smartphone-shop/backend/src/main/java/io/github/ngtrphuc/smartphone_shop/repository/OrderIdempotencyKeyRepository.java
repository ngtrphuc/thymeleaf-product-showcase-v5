package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ngtrphuc.smartphone_shop.model.OrderIdempotencyKey;

public interface OrderIdempotencyKeyRepository extends JpaRepository<OrderIdempotencyKey, Long> {

    Optional<OrderIdempotencyKey> findByUserEmailAndIdempotencyKey(String userEmail, String idempotencyKey);
}
