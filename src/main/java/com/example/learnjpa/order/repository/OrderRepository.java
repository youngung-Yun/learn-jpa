package com.example.learnjpa.order.repository;

import com.example.learnjpa.order.Order;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Order save(Order order);
}
