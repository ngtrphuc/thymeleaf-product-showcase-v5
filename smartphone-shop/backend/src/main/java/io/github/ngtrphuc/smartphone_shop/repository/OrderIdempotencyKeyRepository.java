package io.github.ngtrphuc.smartphone_shop.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.OrderIdempotencyKey;

public interface OrderIdempotencyKeyRepository extends JpaRepository<OrderIdempotencyKey, Long> {

    Optional<OrderIdempotencyKey> findByUserEmailAndIdempotencyKey(String userEmail, String idempotencyKey);

    @Modifying
    @Query("""
            DELETE FROM OrderIdempotencyKey k
            WHERE k.orderId IS NULL
              AND k.createdAt < :cutoff
            """)
    int deleteStalePlaceholders(@Param("cutoff") LocalDateTime cutoff);
}
