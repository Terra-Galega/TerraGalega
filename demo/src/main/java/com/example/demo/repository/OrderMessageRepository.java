package com.example.demo.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.entities.OrderMessage;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Integer> {

    // Todo el chat de un pedido
    List<OrderMessage> findByOrderIdOrderByIdAsc(Integer orderId);
    // Último mensaje del chat (para saber si hay chat y si hay mensajes nuevos)
    Optional<OrderMessage> findTopByOrderIdOrderByIdDesc(Integer orderId);
    // Solo los mensajes nuevos (para el polling)
    List<OrderMessage> findByOrderIdAndIdGreaterThanOrderByIdAsc(Integer orderId, Integer afterId);
}