package com.example.demo.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Una línea de un pedido: un producto, su cantidad y su precio en el momento
 * de la compra. Un pedido (Order) tiene una o varias de estas líneas.
 */
@Getter
@Setter
// Se excluyen "order" y "orderDetailAddOns" del toString para evitar bucles
// infinitos entre entidades que se referencian entre sí
@ToString(exclude = { "order", "orderDetailAddOns" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class OrderDetail {

    // Identificador de la línea, autogenerado por la base de datos
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Pedido al que pertenece la línea. @JsonIgnore evita un bucle infinito al
    // convertir a JSON (Order -> details -> order -> details ...)
    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    // Producto que se pidió
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    // Cuántas unidades del producto se pidieron
    @Column(nullable = false)
    private Integer quantity;

    // Precio del producto al momento de pedir. Se copia aquí para que el pedido
    // no cambie si después se modifica el precio del producto.
    @Column(nullable = false)
    private Double unitPrice;

    // Filas de la tabla de unión con los adicionales elegidos
    @JsonIgnore
    @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderDetailAddOn> orderDetailAddOns = new ArrayList<>();

    // Atajo para las vistas: la lista de AddOn elegidos para
    // este plato, sin que la plantilla tenga que navegar la tabla de unión.
    // No es una columna ni un campo mapeado por JPA
    public List<AddOn> getAddOns() {
        return orderDetailAddOns.stream()
                .map(OrderDetailAddOn::getAddOn)
                .collect(Collectors.toList());
    }
}