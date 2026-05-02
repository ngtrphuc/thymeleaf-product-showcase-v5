package io.github.ngtrphuc.smartphone_shop.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.EmailVerificationToken;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByTokenAndUsedFalse(String token);

    long countByUserIdAndCreatedAtAfter(Long userId, LocalDateTime createdAt);

    @Modifying
    @Query("update EmailVerificationToken evt set evt.used = true where evt.user.id = :userId and evt.used = false")
    int markAllUnusedAsUsed(@Param("userId") Long userId);
}
