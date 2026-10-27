package com.example.demo.dto;

import com.example.demo.entities.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

//Clase porque es lo único que el frontend necesita al iniciar sesión. (Auth... User general)
@Getter
// Genera un constructor que recibe todos los atributos.
@AllArgsConstructor
// Omite de la respuesta JSON los atributos cuyo valor sea null.
@JsonInclude(JsonInclude.Include.NON_NULL) // admin y operador no tienen dirección
public class LoginResponseDTO {

    // Identificador del usuario que inició sesión.
    private Integer id;

    // Nombre del usuario.
    private String name;

    // Apellido del usuario.
    private String lastName;

    // Dirección del usuario; puede ser null para administradores y operadores.
    private String address;

    // Rol del usuario, utilizado por el frontend para identificar su tipo de cuenta.
    private Role role;
}