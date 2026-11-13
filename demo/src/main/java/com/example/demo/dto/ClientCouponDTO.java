package com.example.demo.dto;

import java.time.LocalDateTime;

import com.example.demo.entities.DiscountType;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Cupón tal como lo ve el cliente en "Mis cupones"
@Getter
@Setter
@NoArgsConstructor
public class ClientCouponDTO {
    private Integer id;
    private String code;
    private String description;
    private DiscountType discountType;
    private Double discountValue;
    private Double maxDiscount;
    private Double minOrderAmount;
    private LocalDateTime startsAt;
    private LocalDateTime expiresAt;
    // true = cupón general (sirve para cualquier cliente con el código)
    private boolean general;
    private ClientCouponStatus status;
}