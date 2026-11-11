package com.example.demo.movil.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Carrito compartido entre web y app. Vive en PostgreSQL; productId y clientId
 * son ids de H2.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "cart_lines", uniqueConstraints = @UniqueConstraint(columnNames = { "client_id", "line_id" }))
public class CartLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "client_id", nullable = false)
    private Integer clientId;

    // "productId:addOnId-addOnId" con los ids ordenados (igual que la web)
    @Column(name = "line_id", nullable = false, length = 200)
    private String lineId;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    // ids de adicionales separados por coma; se repiten si se pide más de uno
    // ("3,3,5")
    @Column(name = "add_on_ids", length = 500)
    private String addOnIds;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}