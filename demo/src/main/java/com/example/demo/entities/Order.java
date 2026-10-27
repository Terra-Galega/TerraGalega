package com.example.demo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//Entidad que representa un pedido realizado por un cliente.
@Getter
@Setter
// "details" se excluye del toString para evitar cargar toda la lista
@ToString(exclude = { "details" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    // Identificador del pedido, autogenerado por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Solo los clientes hacen pedidos y no tiene relación
    // directa entre Order y Admin/Operator/DeliveryPerson, así que se asume que
    // "user" = "client" en la tabla Order.
    // Es nullable porque al eliminar la cuenta de un cliente el pedido se
    // conserva en el historial pero sin cliente
    @JsonIgnoreProperties("password")
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;

    // Estado actual del pedido. Se guarda como texto (ej. "PENDING")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    // Fecha y hora en que el cliente hizo el pedido
    @Column(nullable = false)
    private LocalDateTime createdAt;

    // nullable — se llena cuando el domiciliario marca el pedido como
    // entregado. Todavía no hay pantalla que lo setee, queda listo para cuando
    // exista
    @Column(nullable = true)
    private LocalDateTime deliveredAt;

    // Nullable: se asigna después de creado el pedido, no al momento de
    // pedir
    @JsonIgnoreProperties("password")
    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = true)
    private Operator operator;

    // Nullable: se asigna cuando el operador envía el pedido (estado ON_THE_WAY)
    @JsonIgnoreProperties("password")
    @ManyToOne
    @JoinColumn(name = "delivery_person_id", nullable = true)
    private DeliveryPerson deliveryPerson;

    // Dirección de entrega escrita por el cliente en el checkout
    @Column(nullable = false, length = 120)
    private String address;

    // Costo del domicilio cobrado en este pedido (null en los pedidos anteriores)
    @Column(nullable = true)
    private Double deliveryFee;

    // Líneas del pedido. "mappedBy" indica que la FK vive en OrderDetail.order.
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetail> details = new ArrayList<>();

    // Suma de los productos y adicionales, sin domicilio
    // Fórmula por línea: (precio unitario + suma de adicionales) * cantidad
    public Double getSubtotal() {
        return details.stream()
                .mapToDouble(d -> {
                    double addOnsPrice = d.getAddOns().stream()
                            .mapToDouble(AddOn::getPrice)
                            .sum();
                    return (d.getUnitPrice() + addOnsPrice) * d.getQuantity();
                })
                .sum();
    }

    // No es columna, se calcula para que nunca quede desactualizado
    // Total = subtotal + domicilio (los pedidos viejos sin domicilio cuentan 0)
    public Double getTotal() {
        return getSubtotal() + (deliveryFee != null ? deliveryFee : 0.0);
    }
}