package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

//Un ítem del carrito que el frontend envía al hacer checkout.
@Getter
@Setter
@NoArgsConstructor
public class CheckoutItemDTO {

    // Producto elegido
    private Integer productId;
    // Unidades pedidas (debe ser >= 1)
    private Integer quantity;
    // Ids de los adicionales elegidos para este producto (puede venir null)
    private List<Integer> addOnIds;
}