package com.example.demo.entities;

/***
 * Estados por los que pasa un pedido.
 *
 * Flujo normal:
 * PENDING -> CONFIRMED -> PREPARING -> ON_THE_WAY -> DELIVERED
 *
 * El cliente solo puede cancelar mientras está PENDING (CANCELLED).
 */
public enum OrderStatus {

    // Recién creado por el cliente, el restaurante aún no lo confirma.
    PENDING,
    // El operador confirmó el pedido.
    CONFIRMED,
    // El operador lo está cocinando.
    PREPARING,
    // Un domiciliario lo lleva al cliente.
    ON_THE_WAY,
    // Entregado al cliente (estado final).
    DELIVERED,
    // Cancelado por el cliente antes de ser confirmado (estado final).
    CANCELLED

}