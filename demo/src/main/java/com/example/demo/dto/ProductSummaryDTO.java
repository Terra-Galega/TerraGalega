package com.example.demo.dto;

// Producto para listados: la categoría va resumida (id y nombre, sin descripción).
// El detalle de un producto sigue usando ProductDTO con la categoría completa.
// Es un record: una clase inmutable que ya trae constructor y getters.
public record ProductSummaryDTO(
                Integer id,
                String name,
                String description,
                Double price,
                CategoryRefDTO category,
                String imageUrl,
                // true = visible en el menú; false = oculto
                Boolean active,
                // true = aparece en "Favoritos de la casa"
                Boolean popular,
                // Características del plato (filtros del menú)
                boolean vegetarian,
                boolean spicyMild,
                boolean spicyHot,
                // Alérgenos
                boolean containsNuts,
                boolean containsSeafood,
                boolean containsGluten) {
}