package com.example.demo.dto;

import com.example.demo.entities.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Respuesta del login, del registro y de la edición del perfil: el JWT y los
// datos básicos para que el front muestre el nombre y sepa a qué página ir.
// Nunca incluye la contraseña.
@Getter
@AllArgsConstructor
public class AuthResponseDTO {

    // El front lo envía en cada petición en el header Authorization
    private String token;

    // Id del perfil (cliente, admin u operador), no el de la tabla users
    private Integer id;

    private String name;

    private String lastName;

    private Role role;
}
