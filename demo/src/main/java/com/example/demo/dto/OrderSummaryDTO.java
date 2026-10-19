package com.example.demo.dto;

import com.example.demo.entities.OrderStatus;

import java.time.LocalDateTime;

public record OrderSummaryDTO(
        Integer id,
        OrderStatus status,
        LocalDateTime createdAt,
        String address,
        String clientName,
        String deliveryPersonName,
        Double total) {
}