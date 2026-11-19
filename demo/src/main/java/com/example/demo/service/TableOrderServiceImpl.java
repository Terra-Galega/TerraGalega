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

    // =========================
    // Mesero
    // =========================

    @Override
    @Transactional(readOnly = true)
    public List<WaiterTableDTO> tablesByFloor(Integer floor) {
        if (floor == null || floor < 1) {
            throw ApiException.badRequest("Elige un piso válido.");
        }
        List<DiningTable> floorTables = tables.findByFloorAndAvailableTrueOrderByIdAsc(floor);
        if (floorTables.isEmpty()) {
            throw ApiException.notFound("No hay mesas en el piso " + floor + ".");
        }

        // Pedidos en curso de todas las mesas, agrupados por mesa
        Map<Integer, List<TableOrder>> activeByTable = orders.findWithItemsByStatusIn(ACTIVE).stream()
                .collect(Collectors.groupingBy(o -> o.getTable().getId()));

        Map<Integer, Reservation> upcoming = upcomingReservations(floor);

        return floorTables.stream().map(t -> {
            List<TableOrder> active = activeByTable.getOrDefault(t.getId(), List.of());
            Reservation next = upcoming.get(t.getId());
            return new WaiterTableDTO(
                    t.getId(),
                    t.getFloor(),
                    t.getSeats(),
                    Boolean.TRUE.equals(t.getAttended()),
                    active.size(),
                    active.stream().anyMatch(TableOrder::isReady),
                    next != null ? next.getTime() : null,
                    next != null ? next.getName() : null);
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

    @Override
    @Transactional(readOnly = true)
    public List<TableOrderDTO> activeOrdersOfTable(Integer tableId) {
        if (tableId == null || !tables.existsById(tableId)) {
            throw ApiException.notFound("La mesa no existe.");
        }
        return orders.findWithItemsByTableAndStatusIn(tableId, ACTIVE).stream()
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

        DiningTable table = tables.findById(request.tableId())
                .orElseThrow(() -> ApiException.notFound("La mesa " + request.tableId() + " no existe."));
        if (!Boolean.TRUE.equals(table.getAvailable())) {
            throw ApiException.conflict("La mesa " + table.getId() + " no está en servicio.");
        }

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

        // Un pedido nuevo deja la mesa pendiente de atender
        table.setAttended(false);
        tables.save(table);

        return toDTO(orders.save(order));
    }

    @Override
    @Transactional
    public TableOrderDTO deliver(Integer orderId) {
        TableOrder order = lock(orderId);

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
    public TableOrderDTO cancel(Integer orderId) {
        TableOrder order = lock(orderId);

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
    // (con varias rondas, se espera a que terminen todas)
    private void closeTableIfFree(TableOrder order) {
        DiningTable table = order.getTable();
        boolean othersActive = orders.existsByTableIdAndStatusInAndIdNot(table.getId(), ACTIVE, order.getId());
        if (!othersActive) {
            table.setAttended(true);
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

        return toDTO(orders.save(order));
    }

    // =========================
    // Privados
    // =========================

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
                items);
    }
}
