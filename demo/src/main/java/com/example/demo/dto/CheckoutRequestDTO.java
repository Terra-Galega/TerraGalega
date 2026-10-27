package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

// Cuerpo de la petición POST /checkout: la dirección de entrega y los ítems del carrito. 
@Getter
@Setter
@NoArgsConstructor
public class CheckoutRequestDTO {

    // Dirección a la que se enviará el pedido
    private String address;
    // Productos del carrito (no puede estar vacío)
    private List<CheckoutItemDTO> items;
}