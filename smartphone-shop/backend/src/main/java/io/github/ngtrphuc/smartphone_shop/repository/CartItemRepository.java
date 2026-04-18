package io.github.ngtrphuc.smartphone_shop.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.CartItemEntity;
public interface CartItemRepository extends JpaRepository<CartItemEntity, Long> {
    List<CartItemEntity> findByUserEmail(String userEmail);
    Optional<CartItemEntity> findByUserEmailAndProductId(String userEmail, Long productId);
    @Query("SELECT DISTINCT c.userEmail FROM CartItemEntity c")
    List<String> findDistinctUserEmails();
    void deleteByUserEmail(String userEmail);
    void deleteByUserEmailAndProductId(String userEmail, Long productId);
    void deleteByProductId(Long productId);

    @Modifying
    @Query("UPDATE CartItemEntity c SET c.quantity = :qty WHERE c.userEmail = :email AND c.productId = :productId")
    void updateQuantity(@Param("email") String email,
                        @Param("productId") Long productId,
                        @Param("qty") int qty);
}
