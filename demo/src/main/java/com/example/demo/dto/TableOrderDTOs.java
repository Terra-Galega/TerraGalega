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

    // Un plato del pedido nuevo. addOnIds: adicionales de su categoría (opcional)
    public record NewOrderItemDTO(Integer productId, Integer quantity, String notes, List<Integer> addOnIds) {
    }

    // POST /table-orders. El mesero sale del token, no se envía.
    public record NewTableOrderDTO(Integer tableId, List<NewOrderItemDTO> items) {
    }

    // POST /table-orders/{id}/pay (mesero): CASH o CARD
    public record PaymentDTO(com.example.demo.entities.TablePaymentMethod method) {
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
            // Nombres de los adicionales, en orden alfabético
            List<String> addOns,
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
            // Entregado y aún sin cobrar: el mesero elige efectivo o tarjeta
            boolean awaitingPayment,
            com.example.demo.entities.TablePaymentMethod paymentMethod,
            LocalDateTime paidAt,
            List<OrderItemDTO> items) {
    }

    // Pantalla de cocina: pedidos activos agrupados por mesa. Las mesas van en el
    // orden de su pedido más antiguo, y dentro de cada mesa, del más antiguo al más nuevo.
    public record KitchenTableDTO(Integer tableId, Integer floor, List<TableOrderDTO> orders) {
    }

    // Bandeja del mesero (mismos campos que las notificaciones del cliente, para
    // que la app use la misma pantalla)
    public record WaiterNotificationDTO(
            Integer id,
            String type,
            String title,
            String message,
            Integer orderId,
            Integer tableId,
            boolean read,
            LocalDateTime createdAt) {
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
            // Pedidos entregados que faltan por cobrar. La mesa está libre cuando
            // activeOrders y awaitingPayment son 0.
            int awaitingPayment,
            // Ocupada desde que el mesero toma el pedido hasta que se cobra todo
            boolean occupied,
            // Reserva de hoy en las próximas 2 horas (null si no hay): el aviso
            LocalTime upcomingReservation,
            String upcomingReservationName,
            // Reserva cuyo turno está en curso ahora mismo (null si no hay): la mesa
            // está reservada; normalmente el pedido es de quien reservó
            LocalTime currentReservation,
            String currentReservationName,
            // Mesero que atiende la mesa ocupada (null si está libre)
            Integer attendingWaiterId,
            String attendingWaiterName,
            // true = la atiende quien consulta; false = otro mesero (no puede entrar)
            boolean mine) {
    }
}
