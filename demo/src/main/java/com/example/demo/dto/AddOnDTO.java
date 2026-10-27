package com.example.demo.dto;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * DTO completo de un adicional, tal como lo ven y lo envían el admin y el front
 * en el CRUD. Incluye las categorías donde aparece, cada una como referencia
 * corta (id y nombre). Lombok genera getters, setters y el constructor vacío.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddOnDTO {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    // true = disponible para los clientes; false = oculto
    private Boolean active;
    // Categorías en las que aparece el adicional (puede estar vacío)
    private Set<CategoryRefDTO> categories;
}