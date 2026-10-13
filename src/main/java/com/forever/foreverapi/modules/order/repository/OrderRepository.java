package com.forever.foreverapi.modules.order.repository;

import com.forever.foreverapi.modules.order.model.Order;
import com.forever.foreverapi.modules.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByUserId(Long userId, Pageable pageable);

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    @Query("select o.status, count(o) from Order o group by o.status")
    List<Object[]> countGroupedByStatus();

    @Query("select coalesce(sum(o.total), 0) from Order o where o.status <> :excluded")
    BigDecimal sumTotalExcluding(OrderStatus excluded);
}
