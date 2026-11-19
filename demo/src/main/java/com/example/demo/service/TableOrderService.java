package com.example.demo.service;

import java.util.List;

import com.example.demo.dto.TableOrderDTOs.KitchenTableDTO;
import com.example.demo.dto.TableOrderDTOs.NewTableOrderDTO;
import com.example.demo.dto.TableOrderDTOs.TableOrderDTO;
import com.example.demo.dto.TableOrderDTOs.WaiterTableDTO;
import com.example.demo.entities.OrderItemStatus;

// Pedidos de mesa: el mesero los registra en la app y la cocina los prepara.
public interface TableOrderService {

    // ---------- Mesero (app móvil) ----------

    // Mesas en servicio de un piso, con sus pedidos en curso y el aviso de reserva
    List<WaiterTableDTO> tablesByFloor(Integer floor);

    // Pedidos en curso y por cobrar de una mesa
    List<TableOrderDTO> activeOrdersOfTable(Integer tableId);

    // Registra un pedido (start_time = ahora, PENDING) y deja la mesa sin atender
    TableOrderDTO create(Integer waiterId, NewTableOrderDTO request);

    // El mesero entrega el pedido: DONE, end_time = ahora y, si a la mesa no le
    // queda nada en curso, la mesa queda atendida. Todo en una transacción.
    TableOrderDTO deliver(Integer orderId);

    // Cobra un pedido entregado (efectivo o tarjeta). Si a la mesa no le queda
    // nada en cocina ni por cobrar, vuelve a quedar libre.
    TableOrderDTO pay(Integer orderId, com.example.demo.entities.TablePaymentMethod method);

    // "Cobrar mesa": cobra de una vez todos los pedidos entregados y sin cobrar
    // de la mesa, con el mismo método. Devuelve los pedidos cobrados.
    List<TableOrderDTO> payTable(Integer tableId, com.example.demo.entities.TablePaymentMethod method);

    // Cancela el pedido: solo si ningún plato se empezó a preparar
    TableOrderDTO cancel(Integer orderId);

    // Bandeja del mesero: avisos de la cocina sobre sus pedidos
    List<com.example.demo.dto.TableOrderDTOs.WaiterNotificationDTO> notifications(Integer waiterId);

    void markNotificationRead(Integer waiterId, Integer notificationId);

    void markAllNotificationsRead(Integer waiterId);

    // ---------- Cocina (portal web de cocina) ----------

    // Pedidos en curso agrupados por mesa y ordenados por hora de llegada
    List<KitchenTableDTO> kitchen();

    // Avanza un plato: PENDING -> PREPARING -> READY
    TableOrderDTO updateItemStatus(Integer orderId, Integer itemId, OrderItemStatus status);
}
