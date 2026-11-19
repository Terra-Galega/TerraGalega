package com.example.demo.entities;

// Estado de cada plato de un pedido de mesa (OrderItem). La cocina los avanza
// uno a uno y solo hacia delante: PENDING -> PREPARING -> READY.
public enum OrderItemStatus {
    // En espera: la cocina aún no lo empieza (el pedido todavía se puede cancelar)
    PENDING,
    // En preparación
    PREPARING,
    // Listo, esperando en cocina a que el mesero lo recoja
    READY
}
