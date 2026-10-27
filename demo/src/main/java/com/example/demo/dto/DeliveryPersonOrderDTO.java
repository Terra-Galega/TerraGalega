package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Datos del domiciliario que se muestran dentro de un pedido, para que el
 * cliente y el operador sepan quién lo lleva y cómo contactarlo.
 */
@Getter
@Setter
@NoArgsConstructor
public class DeliveryPersonOrderDTO {
    private Integer id;
    private String name;
    private String lastName;
    private String phone;
}