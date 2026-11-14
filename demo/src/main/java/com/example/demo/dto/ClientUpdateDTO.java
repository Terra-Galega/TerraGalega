package com.example.demo.dto;

import com.example.demo.entities.Client;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos que el cliente puede cambiar desde "Editar cuenta".
// El rol, la contraseña y las direcciones no se reciben por aquí.
// Si el front envía campos de más, se ignoran.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClientUpdateDTO {

    private String name;
    private String lastName;
    private String email;
    private String phone;

    public Client toEntity() {
        return Client.builder()
                .name(name)
                .lastName(lastName)
                .email(email)
                .phone(phone)
                .build();
    }
}
