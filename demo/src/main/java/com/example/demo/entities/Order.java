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

import com.fasterxml.jackson.annotation.JsonIncludeProperties;

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
    @JsonIncludeProperties({ "id", "name", "lastName" })
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
    @JsonIncludeProperties({ "id", "name", "lastName" })
    @ManyToOne
    @JoinColumn(name = "operator_id", nullable = true)
    private Operator operator;

    // Nullable: se asigna cuando el operador envía el pedido (estado ON_THE_WAY)
    @JsonIncludeProperties({ "id", "name", "lastName" })
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

    // Cupón aplicado a este pedido (null si no se usó ninguno). Al convertir a
    // JSON solo se incluyen id y código.
    @JsonIncludeProperties({ "id", "code" })
    @ManyToOne
    @JoinColumn(name = "coupon_id", nullable = true)
    private Coupon coupon;

    // Valor descontado, guardado al momento del pedido para que no cambie si el
    // cupón se edita después (null en pedidos sin cupón o anteriores)
    @Column(nullable = true)
    private Double discountAmount;

    // Referencia estable para que un reintento no cree otro pago.
    @Column(unique = true, length = 100)
    private String paymentReference;

    private String paymentRequestHash;

    // URL de vuelta de Stripe sin el id del pedido (web o app móvil)
    @Column(length = 300)
    private String paymentReturnUrl;
    private String paymentState;
    private Long paymentAmount;
    private Long paymentExpiresAt;

    @Column(unique = true)
    private String stripeSessionId;

    @Column(length = 4096)
    private String stripeCheckoutUrl;

    private String stripePaymentIntent;
    private String stripeRefundId;

    // Conserva el subtotal calculado al crear el pedido.
    private Double checkoutSubtotal;

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isVisibleOrder() {
        return paymentReference == null
                || !("PENDING".equals(paymentState)
                        || "EXPIRED".equals(paymentState));
    }

    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isProcessable() {
        return paymentReference == null || "PAID".equals(paymentState);
    }

    // Suma de los productos y adicionales, sin domicilio
    // Fórmula por línea: (precio unitario + suma de adicionales) * cantidad
    public Double getSubtotal() {
        if (checkoutSubtotal != null) {
            return checkoutSubtotal;
        }

        return details.stream()
                .mapToDouble(detail -> {
                    double addOnsPrice = detail.getOrderDetailAddOns().stream()
                            .mapToDouble(link -> link.getUnitPrice() != null
                                    ? link.getUnitPrice()
                                    : link.getAddOn().getPrice())
                            .sum();

                    return (detail.getUnitPrice() + addOnsPrice)
                            * detail.getQuantity();
                })
                .sum();
    }

    // No es columna, se calcula para que nunca quede desactualizado
    // Total = subtotal - descuento + domicilio (los pedidos viejos sin descuento
    // ni domicilio cuentan 0)
    public Double getTotal() {
        return getSubtotal()
                - (discountAmount != null ? discountAmount : 0.0)
                + (deliveryFee != null ? deliveryFee : 0.0);
    }
}