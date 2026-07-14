package com.example.learnjpa.order.repository;

import com.example.learnjpa.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Order save(Order order);

    @Override
    Optional<Order> findById(Long id);

    @Query("""
        SELECT DISTINCT o
        FROM Order o
        JOIN FETCH o.member m
        JOIN FETCH o.orderProductList op
        JOIN FETCH op.product
        WHERE o.id = :id
    """)
    Optional<Order> findDetailById(@Param("id") Long id);
}
