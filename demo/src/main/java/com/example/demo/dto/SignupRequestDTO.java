package com.example.demo.dto;

import java.util.List;

import com.example.demo.entities.Address;
import com.example.demo.entities.Client;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos necesarios para registrarse. El rol no se recibe:
// el servidor siempre crea la cuenta como CLIENT.
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SignupRequestDTO {

    private String name;
    private String lastName;
    private String email;
    private String password;
    private String phone;
    // Al menos una dirección; el nombre es opcional
    private List<AddressDTO> addresses;

    public Client toEntity() {
        Client client = Client.builder()
                .name(name)
                .lastName(lastName)
                .email(email)
                .password(password)
                .phone(phone)
                .build();

        if (addresses != null) {
            for (AddressDTO address : addresses) {
                client.getAddresses().add(Address.builder()
                        .name(address.getName())
                        .label(address.getLabel())
                        .build());
            }
        }
        return client;
    }
}
