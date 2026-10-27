package com.example.demo.dto;

// Producto para listados: la categoría va resumida (id y nombre, sin descripción).
// El detalle de un producto sigue usando ProductDTO con la categoría completa.
public record ProductSummaryDTO(
        Integer id,
        String name,
        String description,
        Double price,
        CategoryRefDTO category,
        String imageUrl,
        Boolean active,
        Boolean popular,
        boolean vegetarian,
        boolean spicyMild,
        boolean spicyHot,
        boolean containsNuts,
        boolean containsSeafood,
        boolean containsGluten) {
}