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

@Getter
@Setter
@ToString(exclude = { "details" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    //Solo los clientes hacen pedidos y  no tiene relación
    // directa entre Order y Admin/Operator/DeliveryPerson, así que se asume que
    // "user" = "client" en la tabla Order.
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    // nullable — se llena cuando el domiciliario marca el pedido como
    // entregado. Todavía no hay pantalla que lo setee, queda listo para cuando
    // exista
    @Column(nullable = true)
    private LocalDateTime deliveredAt;

    // Nullable: se asigna después de creado el pedido, no al momento de
    // pedir
    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = true)
    private Operator operator;

    @ManyToOne
    @JoinColumn(name = "delivery_person_id", nullable = true)
    private DeliveryPerson deliveryPerson;

    @Column(nullable = false, length = 120)
    private String address;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetail> details = new ArrayList<>();

    // No es columna (no está en el ERD): se calcula a partir de los
    // OrderDetail en vez de guardarse, para que nunca quede desactualizado
    // si cambia un detalle
    public Double getTotal() {
        return details.stream()
                .mapToDouble(d -> {
                    double addOnsPrice = d.getAddOns().stream()
                            .mapToDouble(AddOn::getPrice)
                            .sum();
                    return (d.getUnitPrice() + addOnsPrice) * d.getQuantity();
                })
                .sum();
    }
}