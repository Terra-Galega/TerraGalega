package com.example.demo.repository;

import com.example.demo.entities.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.example.demo.entities.OrderStatus;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

    List<Order> findByClientIdOrderByCreatedAtDesc(Integer clientId);

    boolean existsByDetailsProductIdAndStatusNot(Integer productId, OrderStatus status);
}