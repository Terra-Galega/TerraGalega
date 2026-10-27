package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que ve el operador al elegir un domiciliario para enviar un pedido
@Getter
@Setter
@NoArgsConstructor
public class DeliveryPersonOptionDTO {
    private Integer id;
    private String name;
    private String lastName;
    private String phone;
}