package com.example.demo.movil.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_mirror_items")
public class OrderMirrorItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_mirror_id", nullable = false)
    private OrderMirror order;

    @Column(name = "product_id", nullable = false)
    private Integer productId;

    @Column(name = "product_name", nullable = false, length = 120)
    private String productName;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "unit_price", nullable = false)
    private double unitPrice;

    // Ej.: "x2 Queso extra, x1 Salsa"
    @Column(name = "add_ons", length = 500)
    private String addOns;

    // Suma de los adicionales de UNA unidad
    @Column(name = "add_ons_price", nullable = false)
    private double addOnsPrice;

    @Column(name = "line_total", nullable = false)
    private double lineTotal;
}