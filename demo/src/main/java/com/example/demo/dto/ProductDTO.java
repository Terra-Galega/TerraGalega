package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * DTO completo de un producto, para el detalle y para crear o editar desde el
 * admin. La categoría va completa (con descripción). Para los listados se usa
 * ProductSummaryDTO, que es más liviano.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductDTO {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private CategoryDTO category;
    private String imageUrl;
    // true = visible en el menú; false = oculto
    private Boolean active;
    // true = aparece en "Favoritos de la casa"
    private Boolean popular;
    // Características del plato (filtros del menú)
    private boolean vegetarian;
    private boolean spicyMild;
    private boolean spicyHot;
    // Alérgenos
    private boolean containsNuts;
    private boolean containsSeafood;
    private boolean containsGluten;
}