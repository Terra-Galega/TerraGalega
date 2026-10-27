package com.example.demo.dto;

// Producto reducido a id y nombre, para llenar desplegables de filtro
// (por ejemplo, el filtro por producto de los pedidos del admin).
// Es un record: una clase inmutable que ya trae constructor y getters.
public record ProductOptionDTO(
                Integer id,
                String name) {
}