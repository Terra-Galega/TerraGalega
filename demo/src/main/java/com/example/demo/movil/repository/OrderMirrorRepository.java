package com.example.demo.movil.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.movil.entity.OrderMirror;

@Repository
public interface OrderMirrorRepository extends JpaRepository<OrderMirror, Long> {

    Optional<OrderMirror> findByOrderId(Integer orderId);
}