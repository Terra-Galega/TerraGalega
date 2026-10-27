package com.example.demo.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Adicional tal como aparece dentro de un pedido (en OrderDetailDTO). Guarda
 * solo id, nombre y precio. Se arma en OrderServiceImpl al convertir un pedido
 * a DTO.
 */
@Getter
@Setter
@NoArgsConstructor
public class AddOnOrderDTO {
    private Integer id;
    private String name;
    private Double price;
}