package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<Address> findByIdAndUserId(Long id, Long userId);

    long countByUserId(Long userId);

    Optional<Address> findFirstByUserIdAndIsDefaultTrueOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query("update Address a set a.isDefault = false where a.user.id = :userId and a.isDefault = true")
    int clearDefaultForUser(@Param("userId") Long userId);
}
