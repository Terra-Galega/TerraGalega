package com.example.demo.movil.dto;

import java.time.LocalDateTime;

public final class DeliveryDTOs {

    private DeliveryDTOs() {
    }

    /** Lo que ve el CLIENTE: su PIN y dónde va el repartidor. */
    public record ClientDeliveryDTO(
            Integer orderId,
            String status,
            String address,
            String pin, // solo mientras va en camino
            String deliveryPersonName,
            String deliveryPersonPhone,
            Double latitude,
            Double longitude,
            LocalDateTime locationUpdatedAt) {
    }

    /** Lo que ve el REPARTIDOR: nunca incluye el PIN. */
    public record DeliveryJobDTO(
            Integer orderId,
            String status,
            String address,
            String clientName,
            String clientPhone,
            Double latitude,
            Double longitude,
            LocalDateTime locationUpdatedAt,
            int attemptsLeft) {
    }

    public record LocationRequestDTO(Double latitude, Double longitude) {
    }

    public record CompleteDeliveryRequestDTO(String pin) {
    }
}