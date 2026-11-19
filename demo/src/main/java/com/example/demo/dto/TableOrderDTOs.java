package com.example.demo.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.entities.OrderItemStatus;
import com.example.demo.entities.TableOrderStatus;

// DTOs del módulo de pedidos de mesa (mesero en la app móvil y cocina en la web).
public final class TableOrderDTOs {

    private TableOrderDTOs() {
    }

    // ---------- Lo que envía el mesero ----------

    // Un plato del pedido nuevo
    public record NewOrderItemDTO(Integer productId, Integer quantity, String notes) {
    }

    // POST /table-orders. El mesero sale del token, no se envía.
    public record NewTableOrderDTO(Integer tableId, List<NewOrderItemDTO> items) {
    }

    // PUT /table-orders/{id}/items/{itemId}/status (cocina)
    public record ItemStatusDTO(OrderItemStatus status) {
    }

    // ---------- Respuestas ----------

    public record OrderItemDTO(
            Integer id,
            Integer productId,
            String productName,
            Integer quantity,
            String notes,
            OrderItemStatus status) {
    }

    public record TableOrderDTO(
            Integer id,
            Integer tableId,
            Integer floor,
            Integer waiterId,
            String waiterName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            TableOrderStatus status,
            // Todos los platos listos: esperando en cocina a que el mesero lo recoja
            boolean ready,
            // El mesero aún puede cancelarlo (ningún plato se empezó)
            boolean cancellable,
            List<OrderItemDTO> items) {
    }

    // Pantalla de cocina: pedidos activos agrupados por mesa. Las mesas van en el
    // orden de su pedido más antiguo, y dentro de cada mesa, del más antiguo al más nuevo.
    public record KitchenTableDTO(Integer tableId, Integer floor, List<TableOrderDTO> orders) {
    }

    // Portal del mesero: una mesa del piso elegido
    public record WaiterTableDTO(
            Integer id,
            Integer floor,
            Integer seats,
            // true = ya se le entregó todo
            boolean attended,
            // Pedidos en curso (PENDING o IN_PROGRESS)
            int activeOrders,
            // Alguno de sus pedidos está listo esperando en cocina
            boolean readyToServe,
            // Reserva de hoy en las próximas 2 horas (null si no hay): el aviso
            LocalTime upcomingReservation,
            String upcomingReservationName) {
    }
}
