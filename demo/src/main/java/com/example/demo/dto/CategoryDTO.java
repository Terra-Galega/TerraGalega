package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * DTO completo de una categoría (id, nombre y descripción). Se usa como la
 * categoría de un ProductDTO, es decir, en el detalle de un producto.
 */
@Getter
@Setter
@NoArgsConstructor
public class CategoryDTO {
    private Integer id;
    private String name;
    private String description;
}