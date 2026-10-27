package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Lo que ve el cliente al elegir adicionales de un producto
// (solo lo necesario: sin descripción, estado ni categorías)
@Getter
@Setter
@NoArgsConstructor
public class AddOnOptionDTO {
    private Integer id;
    private String name;
    private Double price;
}