package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dto Producto Parcial: solo el id, el nombre y la imagen, que es lo que muestra la pantalla de pedidos.
@Getter
@Setter
@NoArgsConstructor
public class OrderProductDTO {
    private Integer id;
    private String name;
    private String imageUrl;
}