package com.example.demo.dto;

import com.example.demo.entities.DeliveryPerson;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos que envía el admin al crear o editar un domiciliario.
// El rol y la disponibilidad los decide el servidor.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeliveryPersonRequestDTO {

    private String name;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    private String identification;
    private Boolean active;

    public DeliveryPerson toEntity() {
        return DeliveryPerson.builder()
                .name(name)
                .lastName(lastName)
                .email(email)
                .password(password)
                .phone(phone)
                .identification(identification)
                .active(active)
                .build();
    }
}
