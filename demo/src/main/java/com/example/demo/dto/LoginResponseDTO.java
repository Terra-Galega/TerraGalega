package com.example.demo.dto;

import com.example.demo.entities.Role;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

// se creo esta pq es lo único que el frontend necesita al iniciar sesión
@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) // admin y operador no tienen dirección
public class LoginResponseDTO {

    private Integer id;
    private String name;
    private String lastName;
    private String address;
    private Role role;
}