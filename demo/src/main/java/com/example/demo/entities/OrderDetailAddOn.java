package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Tabla de unión entre una línea de pedido (OrderDetail) y un adicional(AddOn).

@Getter
@Setter
// Se excluye "orderDetail" del toString para evitar un bucle infinito
@ToString(exclude = { "orderDetail" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class OrderDetailAddOn {

    // Identificador de la fila, autogenerado por la base de datos.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Línea del pedido a la que pertenece el adicional
    @ManyToOne
    @JoinColumn(name = "order_detail_id", nullable = false)
    private OrderDetail orderDetail;

    // Adicional que se eligió
    @ManyToOne
    @JoinColumn(name = "add_on_id", nullable = false)
    private AddOn addOn;
}