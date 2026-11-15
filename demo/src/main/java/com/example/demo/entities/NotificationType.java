package com.example.demo.entities;

// Tipos de notificación que se guardan para el cliente
public enum NotificationType {
    WELCOME,
    ANNOUNCEMENT, // solo la web
    COUPON,
    ORDER_CONFIRMED,
    ORDER_PREPARING,
    ORDER_ON_THE_WAY,
    ORDER_NEAR
}