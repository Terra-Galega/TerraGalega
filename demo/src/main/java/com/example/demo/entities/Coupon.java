package com.example.demo.entities;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIncludeProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Cupón de descuento. Si "client" es null, cualquiera con el código puede
// usarlo; si tiene valor, es un cupón personal de ese cliente.
@Getter
@Setter
@ToString(exclude = { "client" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity 
@Table(name = "coupons")
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Código que escribe el cliente. Se guarda siempre en mayúsculas.
    @Column(unique = true, nullable = false, length = 40)
    private String code;

    @Column(nullable = true, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DiscountType discountType;

    // Porcentaje o monto fijo según discountType. (No se llama "value"
    // porque es palabra reservada en H2.)
    @Column(nullable = false)
    private Double discountValue;

    // Tope del descuento en cupones de porcentaje (null = sin tope)
    @Column(nullable = true)
    private Double maxDiscount;

    // Subtotal mínimo del pedido para poder usarlo (null = sin mínimo)
    @Column(nullable = true)
    private Double minOrderAmount;

    // Vigencia (null = sin límite por ese lado)
    @Column(nullable = true)
    private LocalDateTime startsAt;

    @Column(nullable = true)
    private LocalDateTime expiresAt;

    // Usos totales permitidos (null = ilimitado)
    @Column(nullable = true)
    private Integer maxUses;

    // true = se puede usar; false = desactivado pero conservado
    @Column(nullable = false)
    private Boolean active;

    // Dueño del cupón (null = cupón general). Al convertir a JSON, del cliente
    // solo se incluyen id, nombre y apellido.
    @JsonIncludeProperties({ "id", "name", "lastName" })
    @ManyToOne
    @JoinColumn(name = "client_id", nullable = true)
    private Client client;
}