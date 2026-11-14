package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Respuesta de la validación de un cupón: cuánto descontaría sobre un subtotal
@Getter
@Setter
@NoArgsConstructor
public class CouponDiscountDTO {
    private String code;
    private String description;
    // Monto que se descontaría del subtotal
    private Double discount;
}