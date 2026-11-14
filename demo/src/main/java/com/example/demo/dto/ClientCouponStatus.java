package com.example.demo.dto;

// Estado de un cupón desde el punto de vista del cliente
public enum ClientCouponStatus {
    ACTIVE,     // se puede usar ahora
    EXPIRED,    // venció
    USED,       // el cliente ya lo usó
    EXHAUSTED   // alcanzó su límite global de usos
}