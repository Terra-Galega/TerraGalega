package com.example.demo.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

// DTO Order parcial, para detales lo que necesita el frontend: producto, cantidad, precio unitario y los adicionales elegidos.
@Getter
@Setter
@NoArgsConstructor
public class OrderDetailDTO {
    private Integer id;
    private OrderProductDTO product; 
    private Integer quantity;
    private Double unitPrice;
    private List<AddOnOrderDTO> addOns;
}