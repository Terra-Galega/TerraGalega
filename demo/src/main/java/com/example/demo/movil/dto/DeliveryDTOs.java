package com.example.demo.movil.dto;

import java.time.LocalDateTime;
import java.util.List;

public final class DeliveryDTOs {

        private DeliveryDTOs() {
        }

        // Punto compartido con LatLon de Android.
        public record MapPointDTO(Double lat, Double lon) {
        }

        // Datos enviados al pulsar "Iniciar entrega".
        public record StartDeliveryRequestDTO(List<MapPointDTO> route) {
        }

        // Cliente: incluye su PIN.
        public record ClientDeliveryDTO(
                        Integer orderId,
                        String status,
                        String address,
                        String pin,
                        String deliveryPersonName,
                        String deliveryPersonPhone,
                        Double latitude,
                        Double longitude,
                        LocalDateTime locationUpdatedAt,
                        LocalDateTime startedAt,
                        List<MapPointDTO> route,
                        // Avance del recorrido de 0.0 a 1.0 (null si aún no inició).
                        // Lo calcula el back para que web y móvil avisen igual.
                        Double progress) {
        }

        // Repartidor: nunca recibe el PIN.
        public record DeliveryJobDTO(
                        Integer orderId,
                        String status,
                        String address,
                        String clientName,
                        String clientPhone,
                        Double latitude,
                        Double longitude,
                        LocalDateTime locationUpdatedAt,
                        int attemptsLeft,
                        LocalDateTime startedAt,
                        List<MapPointDTO> route,
                        String paymentState,
                        double cashToCollect) {
        }

        // Operador: seguimiento sin PIN.
        public record OperatorDeliveryDTO(
                        Integer orderId,
                        String address,
                        String deliveryPersonName,
                        String deliveryPersonPhone,
                        Double latitude,
                        Double longitude,
                        LocalDateTime locationUpdatedAt,
                        LocalDateTime startedAt,
                        List<MapPointDTO> route) {
        }

        public record LocationRequestDTO(Double latitude, Double longitude) {
        }

        public record CompleteDeliveryRequestDTO(String pin) {
        }
}