package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Referencia corta a una categoría dentro de un adicional o de un listado de
// productos: solo id y nombre, sin la descripción
@Getter
@Setter
@NoArgsConstructor
public class CategoryRefDTO {
    private Integer id;
    private String name;
}