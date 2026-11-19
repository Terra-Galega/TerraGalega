package com.example.demo.entities;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CheckConstraint;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Plato de un pedido de mesa: tabla intermedia entre TableOrder y Product (el
// plato de la carta). Existe aparte porque cada plato del pedido tiene su
// propia cantidad, su nota ("sin cebolla", "término medio") y su propio estado,
// que la cocina avanza plato por plato.
// No confundir con OrderDetail, que es la línea de un pedido a domicilio.
@Getter
@Setter
@ToString(exclude = { "order", "product", "addOns" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "order_items", indexes = {
        @Index(name = "idx_order_items_order", columnList = "table_order_id"),
        @Index(name = "idx_order_items_status", columnList = "status")
}, check = @CheckConstraint(name = "ck_order_items_quantity", constraint = "quantity >= 1"))
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Pedido al que pertenece (FK table_order_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "table_order_id", nullable = false)
    private TableOrder order;

    // Plato de la carta (FK product_id, como en OrderDetail)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Cuántas unidades del plato (mínimo 1; también lo exige la base de datos)
    @Column(nullable = false)
    private Integer quantity;

    // Especificaciones del cliente (opcional)
    @Column(length = 200)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderItemStatus status;

    // Adicionales del plato (opcionales), solo los de su categoría, igual que en
    // los pedidos a domicilio. Tabla intermedia order_item_add_ons. Es un Set
    // para poder traerlo en la misma consulta que los platos (sin N+1).
    @ManyToMany
    @JoinTable(name = "order_item_add_ons",
            joinColumns = @JoinColumn(name = "order_item_id"),
            inverseJoinColumns = @JoinColumn(name = "add_on_id"))
    @OrderBy("name ASC")
    @Builder.Default
    private Set<AddOn> addOns = new LinkedHashSet<>();
}
