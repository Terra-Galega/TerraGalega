package com.example.demo.movil.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Copia en PostgreSQL de un pedido de H2. Solo la escribe el back al guardar el
 * pedido en H2.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_mirror")
public class OrderMirror {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false, unique = true)
    private Integer orderId;

    @Column(name = "client_id")
    private Integer clientId;

    @Column(name = "client_name", length = 200)
    private String clientName;

    @Column(name = "delivery_person_id")
    private Integer deliveryPersonId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "address", nullable = false, length = 120)
    private String address;

    @Column(name = "subtotal", nullable = false)
    private double subtotal;

    @Column(name = "delivery_fee", nullable = false)
    private double deliveryFee;

    @Column(name = "total", nullable = false)
    private double total;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "synced_at", nullable = false)
    private LocalDateTime syncedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private List<OrderMirrorItem> items = new ArrayList<>();
}