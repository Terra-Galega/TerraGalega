package com.example.demo.entities;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Pedido de una mesa del salón, registrado por un mesero desde la app móvil.
// Relaciones:
// - N a 1 con DiningTable (una mesa tiene muchos pedidos: una ronda por pedido).
// - N a 1 con Waiter (el mesero que lo registró).
// - 1 a N con OrderItem (los platos del pedido).
// Índices: (status, start_time) para la pantalla de cocina, que lista los
// pedidos activos del más antiguo al más nuevo; dining_table_id para buscar
// los pedidos activos de una mesa.
@Getter
@Setter
@ToString(exclude = { "table", "waiter", "items" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "table_orders", indexes = {
        @Index(name = "idx_table_orders_status_start", columnList = "status, start_time"),
        @Index(name = "idx_table_orders_start", columnList = "start_time"),
        @Index(name = "idx_table_orders_table", columnList = "dining_table_id"),
        @Index(name = "idx_table_orders_waiter", columnList = "waiter_id")
})
public class TableOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Mesa a la que pertenece el pedido (FK dining_table_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dining_table_id", nullable = false)
    private DiningTable table;

    // Mesero que registró el pedido (FK waiter_id)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waiter_id", nullable = false)
    private Waiter waiter;

    // Momento en que se registró: lo asigna el servidor. La cocina ordena por aquí.
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // null mientras el pedido está en curso; se asigna al entregarlo (DONE) o
    // cancelarlo. Con start_time da el tiempo total del pedido.
    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TableOrderStatus status;

    // Platos del pedido. Se guardan y borran junto con el pedido. Es un Set
    // (ordenado por id) para poder traer platos y adicionales en una sola
    // consulta sin que los platos salgan repetidos.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id ASC")
    @Builder.Default
    private Set<OrderItem> items = new LinkedHashSet<>();

    // Todos los platos están listos (READY): esperando en cocina al mesero
    public boolean isReady() {
        return !items.isEmpty()
                && items.stream().allMatch(i -> i.getStatus() == OrderItemStatus.READY);
    }

    // Sigue en curso: la cocina lo muestra y el mesero lo ve en la mesa
    public boolean isActive() {
        return status == TableOrderStatus.PENDING || status == TableOrderStatus.IN_PROGRESS;
    }
}
