package com.example.demo.entities;

// Cómo pagó la mesa un pedido ya entregado. Lo registra el mesero en la app;
// con eso el pedido queda cobrado y, si no le queda nada más, la mesa libre.
public enum TablePaymentMethod {
    // Efectivo
    CASH,
    // Tarjeta (datáfono del restaurante)
    CARD
}
