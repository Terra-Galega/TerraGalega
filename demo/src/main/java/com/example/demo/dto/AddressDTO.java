package com.example.demo.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Dirección guardada de un cliente. Al crear o editar se usan name, label y
// defaultAddress (el id lo asigna el back).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private Integer id;

    // Nombre de la dirección; al crear, vacío = el back asigna "Casa"
    private String name;

    // Texto de la dirección
    private String label;

    // true = es la dirección predeterminada del cliente
    private Boolean defaultAddress;
}