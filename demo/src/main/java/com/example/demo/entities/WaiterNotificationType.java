package com.example.demo.entities;

// Avisos que la cocina le manda al mesero que tomó el pedido
public enum WaiterNotificationType {
    // La cocina empezó a preparar un plato
    DISH_PREPARING,
    // Un plato está listo
    DISH_READY,
    // Todo el pedido está listo: hay que llevarlo a la mesa
    ORDER_READY
}
