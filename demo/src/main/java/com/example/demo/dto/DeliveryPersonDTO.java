package com.example.demo.dto;

import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Role;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Datos de un domiciliario que ve el admin. El operador usa
// DeliveryPersonOptionDTO, que trae menos información.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryPersonDTO {

    private Integer id;
    private String name;
    private String lastName;
    private String email;
    private Role role;
    private String phone;
    private String identification;
    private Boolean available;
    private Boolean active;

    public static DeliveryPersonDTO from(DeliveryPerson dp) {
        return new DeliveryPersonDTO(dp.getId(), dp.getName(), dp.getLastName(), dp.getEmail(),
                dp.getRole(), dp.getPhone(), dp.getIdentification(), dp.getAvailable(), dp.getActive());
    }
}
