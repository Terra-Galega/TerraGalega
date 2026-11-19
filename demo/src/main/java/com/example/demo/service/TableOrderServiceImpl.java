package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.TableOrderDTOs.KitchenTableDTO;
import com.example.demo.dto.TableOrderDTOs.NewOrderItemDTO;
import com.example.demo.dto.TableOrderDTOs.NewTableOrderDTO;
import com.example.demo.dto.TableOrderDTOs.OrderItemDTO;
import com.example.demo.dto.TableOrderDTOs.TableOrderDTO;
import com.example.demo.dto.TableOrderDTOs.WaiterTableDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.DiningTable;
import com.example.demo.entities.OrderItem;
import com.example.demo.entities.OrderItemStatus;
import com.example.demo.entities.Product;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.TableOrder;
import com.example.demo.entities.TableOrderStatus;
import com.example.demo.entities.TablePaymentMethod;
import com.example.demo.entities.Waiter;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.DiningTableRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.ReservationRepository;
import com.example.demo.repository.TableOrderRepository;
import com.example.demo.repository.WaiterRepository;

@Service
public class TableOrderServiceImpl implements TableOrderService {

    // Pedidos que siguen en curso: los ve la cocina y bloquean "mesa atendida"
    private static final Set<TableOrderStatus> ACTIVE = Set.of(TableOrderStatus.PENDING,
            TableOrderStatus.IN_PROGRESS);

    // Lo que dura un turno de reserva (los turnos van cada hora y media)
    private static final int RESERVATION_SLOT_MINUTES = ReservationRules.SLOT_MINUTES;

    // Aviso del mesero: reservas que empiezan dentro de este margen (2 h por
    // defecto; se cambia con terra.table-orders.reservation-warning-hours)
    @org.springframework.beans.factory.annotation.Value("${terra.table-orders.reservation-warning-hours:2}")
    private int reservationWarningHours;

    // Límites razonables de un pedido de mesa
    private static final int MAX_LINES = 50;
    private static final int MAX_QUANTITY = 99;
    private static final int NOTES_MAX = 200;
    private static final int MAX_ADD_ONS = 20;

    @Autowired
    private TableOrderRepository orders;

    @Autowired
    private DiningTableRepository tables;

    @Autowired
    private WaiterRepository waiters;

    @Autowired
    private ProductRepository products;

    @Autowired
    private ReservationRepository reservations;

    @Autowired
    private AddOnRepository addOns;

    @Autowired
    private com.example.demo.repository.WaiterNotificationRepository notifications;

    // =========================
    // Mesero
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<WaiterTableDTO> tablesByFloor(Integer waiterId, Integer floor) {
        if (floor == null || floor < 1) {
            throw ApiException.badRequest("Elige un piso válido.");
        }
        List<DiningTable> floorTables = tables.findByFloorOrderByIdAsc(floor);
        if (floorTables.isEmpty()) {
            throw ApiException.notFound("No hay mesas en el piso " + floor + ".");
        }

        // Pedidos en curso de todas las mesas, agrupados por mesa
        Map<Integer, List<TableOrder>> activeByTable = orders.findWithItemsByStatusIn(ACTIVE).stream()
                .collect(Collectors.groupingBy(o -> o.getTable().getId()));

        Map<Integer, Long> awaitingByTable = orders.findAwaitingPayment().stream()
                .collect(Collectors.groupingBy(o -> o.getTable().getId(), Collectors.counting()));

        // Quién atiende cada mesa ocupada: el mesero de su pedido abierto más antiguo
        Map<Integer, Waiter> attending = new java.util.HashMap<>();
        for (TableOrder o : orders.findOpenWithWaiter(ACTIVE)) {
            attending.putIfAbsent(o.getTable().getId(), o.getWaiter());
        }

        Map<Integer, Reservation> upcoming = upcomingReservations(floor);
        Map<Integer, Reservation> current = currentReservations(floor);

        return floorTables.stream().map(t -> {
            List<TableOrder> active = activeByTable.getOrDefault(t.getId(), List.of());
            int awaiting = awaitingByTable.getOrDefault(t.getId(), 0L).intValue();
            Reservation next = upcoming.get(t.getId());
            Reservation now = current.get(t.getId());
            Waiter by = attending.get(t.getId());
            return new WaiterTableDTO(
                    t.getId(),
                    t.getFloor(),
                    t.getSeats(),
                    Boolean.TRUE.equals(t.getAttended()),
                    active.size(),
                    active.stream().anyMatch(TableOrder::isReady),
                    awaiting,
                    !active.isEmpty() || awaiting > 0,
                    next != null ? next.getTime() : null,
                    next != null ? next.getName() : null,
                    now != null ? now.getTime() : null,
                    now != null ? now.getName() : null,
                    by != null ? by.getId() : null,
                    by != null ? fullName(by) : null,
                    by != null && by.getId().equals(waiterId));
        }).toList();
    }

    // Primera reserva de cada mesa del piso entre ahora y dentro de 2 horas.
    // Si la franja cruza la medianoche, se consulta hoy hasta el final y mañana desde el inicio.
    private Map<Integer, Reservation> upcomingReservations(Integer floor) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime limit = now.plusHours(reservationWarningHours);

        List<Reservation> found = new ArrayList<>();
        if (limit.toLocalDate().equals(now.toLocalDate())) {
            found.addAll(reservations.findByTableFloorAndDateAndTimeBetweenOrderByTimeAsc(
                    floor, now.toLocalDate(), now.toLocalTime(), limit.toLocalTime()));
        } else {
            found.addAll(reservations.findByTableFloorAndDateAndTimeBetweenOrderByTimeAsc(
                    floor, now.toLocalDate(), now.toLocalTime(), LocalTime.MAX));
            found.addAll(reservations.findByTableFloorAndDateAndTimeBetweenOrderByTimeAsc(
                    floor, limit.toLocalDate(), LocalTime.MIN, limit.toLocalTime()));
        }

        Map<Integer, Reservation> first = new LinkedHashMap<>();
        for (Reservation r : found) {
            first.putIfAbsent(r.getTable().getId(), r);
        }
        return first;
    }

    // Reserva de hoy cuyo turno empezó hace menos de RESERVATION_SLOT_MINUTES
    private Map<Integer, Reservation> currentReservations(Integer floor) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = now.minusMinutes(RESERVATION_SLOT_MINUTES);
        LocalTime from = start.toLocalDate().equals(now.toLocalDate()) ? start.toLocalTime() : LocalTime.MIN;

        Map<Integer, Reservation> found = new LinkedHashMap<>();
        for (Reservation r : reservations.findByTableFloorAndDateAndTimeBetweenOrderByTimeDesc(
                floor, now.toLocalDate(), from, now.toLocalTime())) {
            found.putIfAbsent(r.getTable().getId(), r);
        }
        return found;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TableOrderDTO> activeOrdersOfTable(Integer waiterId, Integer tableId) {
        if (tableId == null || !tables.existsById(tableId)) {
            throw ApiException.notFound("La mesa no existe.");
        }
        checkAttends(waiterId, tableId);
        return orders.findOpenWithItemsByTable(tableId, ACTIVE).stream()
                .map(this::toDTO)
                .toList();
    }

    @Override
    @Transactional
    public TableOrderDTO create(Integer waiterId, NewTableOrderDTO request) {
        if (request == null || request.tableId() == null) {
            throw ApiException.badRequest("Elige la mesa del pedido.");
        }

        Waiter waiter = waiters.findById(waiterId)
                .orElseThrow(() -> ApiException.notFound("El mesero no existe."));
        if (!Boolean.TRUE.equals(waiter.getActive())) {
            throw ApiException.forbidden("Tu cuenta de mesero está desactivada.");
        }

        // Una mesa ocupada (available = false) sí admite otra ronda
        // Bloqueada mientras se toma el pedido: dos meseros no pueden quedarse la misma mesa
        DiningTable table = tables.lockById(request.tableId())
                .orElseThrow(() -> ApiException.notFound("La mesa " + request.tableId() + " no existe."));
        // Otra ronda sí, pero solo del mesero que ya la atiende
        checkAttends(waiterId, table.getId());

        List<NewOrderItemDTO> lines = request.items();
        if (lines == null || lines.isEmpty()) {
            throw ApiException.badRequest("Agrega al menos un plato al pedido.");
        }
        if (lines.size() > MAX_LINES) {
            throw ApiException.badRequest("El pedido no puede tener más de " + MAX_LINES + " líneas.");
        }

        TableOrder order = TableOrder.builder()
                .table(table)
                .waiter(waiter)
                .startTime(LocalDateTime.now())
                .status(TableOrderStatus.PENDING)
                .build();

        for (NewOrderItemDTO line : lines) {
            if (line == null || line.productId() == null) {
                throw ApiException.badRequest("Cada línea del pedido debe tener un plato.");
            }
            Product product = products.findById(line.productId())
                    .orElseThrow(() -> ApiException.notFound("El plato " + line.productId() + " no existe."));
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw ApiException.conflict("El plato «" + product.getName() + "» no está disponible.");
            }

            Integer quantity = line.quantity();
            if (quantity == null || quantity < 1) {
                throw ApiException.badRequest("La cantidad de «" + product.getName() + "» debe ser al menos 1.");
            }
            if (quantity > MAX_QUANTITY) {
                throw ApiException.badRequest("La cantidad de «" + product.getName() + "» no puede pasar de "
                        + MAX_QUANTITY + ".");
            }

            String notes = line.notes() == null ? null : line.notes().trim();
            if (notes != null && notes.isEmpty()) {
                notes = null;
            }
            if (notes != null && notes.length() > NOTES_MAX) {
                throw ApiException.badRequest("La nota de «" + product.getName() + "» no puede pasar de "
                        + NOTES_MAX + " caracteres.");
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .notes(notes)
                    .status(OrderItemStatus.PENDING)
                    .build();

            // Adicionales: deben existir, estar activos y ser de la categoría del
            // plato (misma regla que el checkout a domicilio). Repetidos cuentan una vez.
            if (line.addOnIds() != null) {
                if (line.addOnIds().size() > MAX_ADD_ONS) {
                    throw ApiException.badRequest("«" + product.getName() + "» no puede llevar más de "
                            + MAX_ADD_ONS + " adicionales.");
                }
                for (Integer addOnId : new java.util.LinkedHashSet<>(line.addOnIds())) {
                    if (addOnId == null) {
                        throw ApiException.badRequest("Hay un adicional sin elegir en «" + product.getName() + "».");
                    }
                    AddOn addOn = addOns.findById(addOnId)
                            .orElseThrow(() -> ApiException.notFound("El adicional " + addOnId + " no existe."));
                    if (!Boolean.TRUE.equals(addOn.getActive())) {
                        throw ApiException.conflict("El adicional «" + addOn.getName() + "» no está disponible.");
                    }
                    boolean allowed = product.getCategory() != null && addOn.getCategories().stream()
                            .anyMatch(c -> c.getId().equals(product.getCategory().getId()));
                    if (!allowed) {
                        throw ApiException.badRequest("«" + addOn.getName() + "» no se puede agregar a «"
                                + product.getName() + "».");
                    }
                    item.getAddOns().add(addOn);
                }
            }
            order.getItems().add(item);
        }

        // Un pedido nuevo deja la mesa pendiente de atender y OCUPADA
        table.setAttended(false);
        table.setAvailable(false);
        tables.save(table);

        return toDTO(orders.save(order));
    }

    @Override
    @Transactional
    public TableOrderDTO deliver(Integer waiterId, Integer orderId) {
        TableOrder order = lock(orderId);
        checkAttends(waiterId, order.getTable().getId());

        if (!order.isActive()) {
            throw ApiException.conflict("Este pedido ya se entregó o se canceló.");
        }
        if (!order.isReady()) {
            throw ApiException.conflict("Aún hay platos en cocina. Entrégalo cuando todos estén listos.");
        }

        order.setStatus(TableOrderStatus.DONE);
        order.setEndTime(LocalDateTime.now());
        closeTableIfFree(order);

        return toDTO(orders.save(order));
    }

    @Override
    @Transactional
    public TableOrderDTO pay(Integer waiterId, Integer orderId, TablePaymentMethod method) {
        if (method == null) {
            throw ApiException.badRequest("Elige el método de pago: efectivo o tarjeta.");
        }
        TableOrder order = lock(orderId);
        checkAttends(waiterId, order.getTable().getId());

        if (order.getStatus() != TableOrderStatus.DONE) {
            throw ApiException.conflict("Solo se cobran pedidos ya entregados en la mesa.");
        }
        if (order.getPaidAt() != null) {
            throw ApiException.conflict("Este pedido ya se cobró.");
        }

        order.setPaymentMethod(method);
        order.setPaidAt(LocalDateTime.now());
        TableOrder saved = orders.save(order);
        refreshAvailability(order.getTable());
        return toDTO(saved);
    }

    @Override
    @Transactional
    public List<TableOrderDTO> payTable(Integer waiterId, Integer tableId, TablePaymentMethod method) {
        if (method == null) {
            throw ApiException.badRequest("Elige el método de pago: efectivo o tarjeta.");
        }
        if (tableId == null || !tables.existsById(tableId)) {
            throw ApiException.notFound("La mesa no existe.");
        }
        checkAttends(waiterId, tableId);

        List<TableOrder> pending = orders.lockAwaitingPaymentByTable(tableId);
        if (pending.isEmpty()) {
            throw ApiException.conflict("La mesa " + tableId + " no tiene pedidos entregados por cobrar.");
        }

        // Todos con la misma hora de pago: fue un solo cobro
        LocalDateTime now = LocalDateTime.now();
        for (TableOrder order : pending) {
            order.setPaymentMethod(method);
            order.setPaidAt(now);
        }
        List<TableOrder> saved = orders.saveAll(pending);
        refreshAvailability(saved.get(0).getTable());
        return saved.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional
    public TableOrderDTO cancel(Integer waiterId, Integer orderId) {
        TableOrder order = lock(orderId);
        checkAttends(waiterId, order.getTable().getId());

        if (!order.isActive()) {
            throw ApiException.conflict("Este pedido ya se entregó o se canceló.");
        }
        // Solo mientras sigue en espera: si la cocina ya empezó algún plato, no
        boolean started = order.getItems().stream()
                .anyMatch(i -> i.getStatus() != OrderItemStatus.PENDING);
        if (order.getStatus() != TableOrderStatus.PENDING || started) {
            throw ApiException.conflict("La cocina ya empezó a preparar este pedido; no se puede cancelar.");
        }

        order.setStatus(TableOrderStatus.CANCELLED);
        order.setEndTime(LocalDateTime.now());
        closeTableIfFree(order);

        return toDTO(orders.save(order));
    }

    // La mesa queda atendida si ya no le queda ningún otro pedido en curso
    // (con varias rondas, se espera a que terminen todas). Además se recalcula
    // si sigue ocupada (al cancelar el único pedido, queda libre).
    private void closeTableIfFree(TableOrder order) {
        DiningTable table = order.getTable();
        boolean othersActive = orders.existsByTableIdAndStatusInAndIdNot(table.getId(), ACTIVE, order.getId());
        if (!othersActive) {
            table.setAttended(true);
            tables.save(table);
        }
        refreshAvailability(table);
    }

    // available = la mesa está libre ahora: nada en cocina ni entregado sin cobrar.
    // La consulta ve los cambios de esta misma transacción (Hibernate guarda antes).
    private void refreshAvailability(DiningTable table) {
        boolean free = !orders.isTableOccupied(table.getId(), ACTIVE);
        if (!Boolean.valueOf(free).equals(table.getAvailable())) {
            table.setAvailable(free);
            tables.save(table);
        }
    }

    // =========================
    // Cocina
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<KitchenTableDTO> kitchen() {
        // La consulta ya viene ordenada por start_time: la primera aparición de
        // cada mesa marca el orden de las mesas (la del pedido más antiguo primero)
        Map<Integer, List<TableOrderDTO>> byTable = new LinkedHashMap<>();
        Map<Integer, Integer> floors = new LinkedHashMap<>();
        for (TableOrder order : orders.findWithItemsByStatusIn(ACTIVE)) {
            Integer tableId = order.getTable().getId();
            byTable.computeIfAbsent(tableId, k -> new ArrayList<>()).add(toDTO(order));
            floors.putIfAbsent(tableId, order.getTable().getFloor());
        }
        return byTable.entrySet().stream()
                .map(e -> new KitchenTableDTO(e.getKey(), floors.get(e.getKey()), e.getValue()))
                .toList();
    }

    @Override
    @Transactional
    public TableOrderDTO updateItemStatus(Integer orderId, Integer itemId, OrderItemStatus status) {
        if (status == null) {
            throw ApiException.badRequest("Indica el nuevo estado del plato.");
        }

        TableOrder order = lock(orderId);
        if (!order.isActive()) {
            throw ApiException.conflict("Este pedido ya se entregó o se canceló.");
        }

        OrderItem item = order.getItems().stream()
                .filter(i -> i.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> ApiException.notFound("Ese plato no es de este pedido."));

        // Solo hacia delante: PENDING -> PREPARING -> READY
        if (status.ordinal() <= item.getStatus().ordinal()) {
            throw ApiException.conflict("El plato ya está en " + label(item.getStatus()) + ".");
        }
        item.setStatus(status);

        // En cuanto la cocina empieza algún plato, el pedido pasa a IN_PROGRESS
        // (y desde ese momento ya no se puede cancelar)
        if (order.getStatus() == TableOrderStatus.PENDING) {
            order.setStatus(TableOrderStatus.IN_PROGRESS);
        }

        // Aviso al mesero que tomó el pedido (en la misma transacción)
        notifyWaiter(order, item);

        return toDTO(orders.save(order));
    }

    @Override
    @Transactional
    public TableOrderDTO updateOrderStatus(Integer orderId, OrderItemStatus status) {
        if (status == null || status == OrderItemStatus.PENDING) {
            throw ApiException.badRequest("Indica si el pedido pasa a preparación o a listo.");
        }

        TableOrder order = lock(orderId);
        if (!order.isActive()) {
            throw ApiException.conflict("Este pedido ya se entregó o se canceló.");
        }

        // Solo se mueven los platos que van por detrás del nuevo estado: con
        // "Preparar todo" los que ya están listos se quedan listos
        List<OrderItem> moved = order.getItems().stream()
                .filter(i -> i.getStatus().ordinal() < status.ordinal())
                .toList();
        if (moved.isEmpty()) {
            throw ApiException.conflict(status == OrderItemStatus.READY
                    ? "Todos los platos de este pedido ya están listos."
                    : "Todos los platos de este pedido ya se están preparando.");
        }
        moved.forEach(i -> i.setStatus(status));

        if (order.getStatus() == TableOrderStatus.PENDING) {
            order.setStatus(TableOrderStatus.IN_PROGRESS);
        }

        // Un solo aviso por el pedido entero, no uno por plato
        notifyWaiterOrder(order, status, moved.size());

        return toDTO(orders.save(order));
    }

    // =========================
    // Bandeja del mesero
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<com.example.demo.dto.TableOrderDTOs.WaiterNotificationDTO> notifications(Integer waiterId) {
        return notifications.findTop50ByWaiterIdOrderByCreatedAtDescIdDesc(waiterId).stream()
                .map(n -> new com.example.demo.dto.TableOrderDTOs.WaiterNotificationDTO(
                        n.getId(), n.getType().name(), n.getTitle(), n.getMessage(),
                        n.getTableOrderId(), n.getTableId(), n.isRead(), n.getCreatedAt()))
                .toList();
    }

    @Override
    @Transactional
    public void markNotificationRead(Integer waiterId, Integer notificationId) {
        com.example.demo.entities.WaiterNotification n = notifications
                .findByIdAndWaiterId(notificationId, waiterId)
                .orElseThrow(() -> ApiException.notFound("El aviso no existe."));
        n.setRead(true);
        notifications.save(n);
    }

    @Override
    @Transactional
    public void markAllNotificationsRead(Integer waiterId) {
        notifications.markAllRead(waiterId);
    }

    // Plato en preparación, plato listo o, si con este plato queda todo listo,
    // "pedido listo para servir"
    private void notifyWaiter(TableOrder order, OrderItem item) {
        Integer tableId = order.getTable().getId();
        String dish = item.getProduct().getName();
        com.example.demo.entities.WaiterNotificationType type;
        String title;
        String message;

        if (order.isReady()) {
            type = com.example.demo.entities.WaiterNotificationType.ORDER_READY;
            title = "Mesa " + tableId + ": pedido listo";
            message = "El pedido #" + order.getId() + " de la mesa " + tableId
                    + " está listo en cocina. Llévalo a la mesa.";
        } else if (item.getStatus() == OrderItemStatus.READY) {
            type = com.example.demo.entities.WaiterNotificationType.DISH_READY;
            title = "Mesa " + tableId + ": plato listo";
            message = "«" + dish + "» (pedido #" + order.getId() + ") está listo.";
        } else {
            type = com.example.demo.entities.WaiterNotificationType.DISH_PREPARING;
            title = "Mesa " + tableId + ": en preparación";
            message = "La cocina empezó «" + dish + "» (pedido #" + order.getId() + ").";
        }

        notifications.save(com.example.demo.entities.WaiterNotification.builder()
                .waiter(order.getWaiter())
                .type(type)
                .title(title)
                .message(message.length() > 255 ? message.substring(0, 255) : message)
                .tableOrderId(order.getId())
                .tableId(tableId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // Aviso cuando la cocina cambia el pedido entero de una vez
    private void notifyWaiterOrder(TableOrder order, OrderItemStatus status, int dishes) {
        Integer tableId = order.getTable().getId();
        com.example.demo.entities.WaiterNotificationType type;
        String title;
        String message;

        if (order.isReady()) {
            type = com.example.demo.entities.WaiterNotificationType.ORDER_READY;
            title = "Mesa " + tableId + ": pedido listo";
            message = "El pedido #" + order.getId() + " de la mesa " + tableId
                    + " está listo en cocina. Llévalo a la mesa.";
        } else {
            type = com.example.demo.entities.WaiterNotificationType.DISH_PREPARING;
            title = "Mesa " + tableId + ": en preparación";
            message = "La cocina empezó " + (dishes == 1 ? "1 plato" : dishes + " platos")
                    + " del pedido #" + order.getId() + ".";
        }

        notifications.save(com.example.demo.entities.WaiterNotification.builder()
                .waiter(order.getWaiter())
                .type(type)
                .title(title)
                .message(message)
                .tableOrderId(order.getId())
                .tableId(tableId)
                .read(false)
                .createdAt(LocalDateTime.now())
                .build());
    }

    // =========================
    // Privados
    // =========================

    // Una mesa ocupada solo la maneja el mesero que la atiende (el de su pedido
    // abierto más antiguo). Libre, cualquiera puede tomarla.
    private void checkAttends(Integer waiterId, Integer tableId) {
        List<TableOrder> open = orders.findOpenWithWaiterByTable(tableId, ACTIVE);
        if (open.isEmpty()) {
            return;
        }
        Waiter by = open.get(0).getWaiter();
        if (!by.getId().equals(waiterId)) {
            throw ApiException.forbidden("La mesa " + tableId + " la atiende " + fullName(by)
                    + ". Solo quien la atiende puede verla y tomar pedidos.");
        }
    }

    private static String fullName(Waiter w) {
        return (w.getName() + " " + (w.getLastName() == null ? "" : w.getLastName())).trim();
    }

    private TableOrder lock(Integer orderId) {
        return orders.lockById(orderId)
                .orElseThrow(() -> ApiException.notFound("El pedido no existe."));
    }

    private static String label(OrderItemStatus status) {
        return switch (status) {
            case PENDING -> "espera";
            case PREPARING -> "preparación";
            case READY -> "listo";
        };
    }

    private TableOrderDTO toDTO(TableOrder o) {
        Waiter w = o.getWaiter();
        List<OrderItemDTO> items = o.getItems().stream()
                .map(i -> new OrderItemDTO(
                        i.getId(),
                        i.getProduct().getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getNotes(),
                        i.getAddOns().stream().map(AddOn::getName).toList(),
                        i.getStatus()))
                .toList();
        boolean cancellable = o.getStatus() == TableOrderStatus.PENDING
                && o.getItems().stream().allMatch(i -> i.getStatus() == OrderItemStatus.PENDING);
        return new TableOrderDTO(
                o.getId(),
                o.getTable().getId(),
                o.getTable().getFloor(),
                w.getId(),
                (w.getName() + " " + w.getLastName()).trim(),
                o.getStartTime(),
                o.getEndTime(),
                o.getStatus(),
                o.isActive() && o.isReady(),
                cancellable,
                o.isAwaitingPayment(),
                o.getPaymentMethod(),
                o.getPaidAt(),
                items);
    }
}
