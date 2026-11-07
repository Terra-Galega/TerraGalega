package com.example.demo.movil.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vive en PostgreSQL. No hay FK reales hacia H2 (son bases distintas):
 * orderId, clientId y deliveryPersonId son ids de orders, clients y
 * delivery_persons de H2.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "delivery_tracking")
public class DeliveryTracking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    // null si la cuenta del cliente se borró
    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "delivery_person_id", nullable = false)
    private Integer deliveryPersonId;

    @Column(name = "pin", nullable = false, length = 4)
    private String pin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private DeliveryTrackingStatus status;

    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;
}