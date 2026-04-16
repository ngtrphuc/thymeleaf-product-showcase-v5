package io.github.ngtrphuc.smartphone_shop.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import io.github.ngtrphuc.smartphone_shop.model.Order;
import jakarta.persistence.LockModeType;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.userEmail = :email ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findByUserEmailOrderByCreatedAtDesc(@Param("email") String email);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findAllByOrderByCreatedAtDesc();

    @Query("SELECT o FROM Order o ORDER BY o.createdAt DESC, o.id DESC")
    List<Order> findRecentOrders(Pageable pageable);

    @Query(
            value = "SELECT o.id FROM Order o ORDER BY o.createdAt DESC, o.id DESC",
            countQuery = "SELECT COUNT(o) FROM Order o"
    )
    Page<Long> findOrderIdsByCreatedAtDesc(Pageable pageable);

    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id IN :ids")
    List<Order> findAllWithItemsByIdIn(@Param("ids") List<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItemsForUpdate(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status != 'cancelled'")
    Double sumRevenueExcludingCancelled();

    @Query("SELECT COALESCE(SUM(oi.quantity), 0) FROM OrderItem oi WHERE oi.order.status != 'cancelled'")
    Long sumItemsSoldExcludingCancelled();
}
