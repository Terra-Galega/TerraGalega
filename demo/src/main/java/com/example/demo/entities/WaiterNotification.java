package com.example.demo.entities;

import java.time.LocalDateTime;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Bandeja del mesero: avisos de la cocina sobre sus pedidos de mesa (un plato
// empezó, un plato está listo, el pedido está listo para servir). La app los
// consulta cada pocos segundos, vibra y los muestra en su bandeja.
// Separada de Notification, que es la bandeja de los clientes.
@Getter
@Setter
@ToString(exclude = "waiter")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "waiter_notifications", indexes = {
        @Index(name = "idx_waiter_notifications_waiter_created", columnList = "waiter_id, created_at")
})
public class WaiterNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Mesero que recibe el aviso (el que tomó el pedido)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "waiter_id", nullable = false)
    private Waiter waiter;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private WaiterNotificationType type;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(nullable = false, length = 255)
    private String message;

    // Pedido y mesa del aviso (para abrir la mesa desde la bandeja)
    @Column(name = "table_order_id", nullable = false)
    private Integer tableOrderId;

    @Column(name = "table_id", nullable = false)
    private Integer tableId;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
