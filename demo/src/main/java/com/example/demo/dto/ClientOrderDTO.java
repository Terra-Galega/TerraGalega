package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// Genera automáticamente los métodos getter y setter.
@Getter
@Setter
// Genera un constructor sin argumentos.
@NoArgsConstructor
// DTO que contiene los datos del cliente necesarios para mostrar información relacionada con un pedido.
public class ClientOrderDTO {

    // Identificador del cliente.
    private Integer id;

    // Nombre del cliente.
    private String name;

    // Apellido del cliente.
    private String lastName;

    // Correo electrónico del cliente.
    private String email;

    // Número de teléfono del cliente.
    private String phone;
}