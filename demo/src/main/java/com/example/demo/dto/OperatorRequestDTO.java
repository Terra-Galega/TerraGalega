package com.example.demo.dto;

import com.example.demo.entities.Operator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos que envía el admin al crear o editar un operador.
// La contraseña solo se usa al crear; al editar se ignora.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class OperatorRequestDTO {

    private String name;
    private String lastName;
    private String email;
    private String username;
    private String password;
    private Boolean active;

    public Operator toEntity() {
        return Operator.builder()
                .name(name)
                .lastName(lastName)
                .email(email)
                .username(username)
                .password(password)
                .active(active)
                .build();
    }
}
