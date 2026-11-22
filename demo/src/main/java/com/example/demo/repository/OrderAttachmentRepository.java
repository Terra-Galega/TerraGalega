package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entities.OrderAttachment;

public interface OrderAttachmentRepository extends JpaRepository<OrderAttachment, Integer> {

    // Para el límite de fotos por pedido
    long countByOrderId(Integer orderId);
}