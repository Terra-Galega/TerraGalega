package com.example.demo.movil.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.delivery.entity.DeliveryTracking;

@Repository
public interface DeliveryTrackingRepository extends JpaRepository<DeliveryTracking, Long> {

    Optional<DeliveryTracking> findByOrderId(Integer orderId);
}