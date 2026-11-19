package com.example.demo.entities;

// Estados de un pedido de mesa (TableOrder). Distinto de OrderStatus, que es el
// de los pedidos a domicilio.
//
// Flujo normal: PENDING -> IN_PROGRESS -> DONE
// - PENDING: recién registrado por el mesero; ningún plato se está preparando.
// - IN_PROGRESS: la cocina empezó al menos un plato (pasa solo).
// - DONE: el mesero entregó el pedido en la mesa (end_time asignado). Sigue
//   visible en la mesa hasta que se cobra (payment_method y paid_at).
// - CANCELLED: el mesero lo canceló mientras seguía en PENDING.
public enum TableOrderStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    CANCELLED
}
