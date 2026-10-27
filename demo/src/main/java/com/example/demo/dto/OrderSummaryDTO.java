package com.example.demo.dto;

import com.example.demo.entities.OrderStatus;

import java.time.LocalDateTime;

/**
 * Resumen plano de un pedido para las tablas del operador y del admin.
 *
 * deliveryPersonName pueden ser null si el pedido no tiene aún no tiene domiciliario.
 */
public record OrderSummaryDTO(
        Integer id,
        OrderStatus status,
        LocalDateTime createdAt,
        String address,
        String clientName,
        String deliveryPersonName,
        Double total) {
}