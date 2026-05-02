package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import io.github.ngtrphuc.smartphone_shop.model.OrderReturn;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Long> {
    Optional<OrderReturn> findByOrderId(Long orderId);

    boolean existsByOrderId(Long orderId);
}
