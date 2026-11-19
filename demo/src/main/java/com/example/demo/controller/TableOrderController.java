package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.TableOrderDTOs.ItemStatusDTO;
import com.example.demo.dto.TableOrderDTOs.KitchenTableDTO;
import com.example.demo.dto.TableOrderDTOs.NewTableOrderDTO;
import com.example.demo.dto.TableOrderDTOs.TableOrderDTO;
import com.example.demo.dto.TableOrderDTOs.WaiterTableDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.TableOrderService;

import io.swagger.v3.oas.annotations.Operation;

// API de pedidos de mesa. Ruta: /table-orders.
// - Mesero (rol WAITER, app móvil): mesas por piso, pedidos de una mesa, crear,
//   entregar, cobrar y cancelar.
// - Cocina (rol KITCHEN, portal web propio): pedidos en curso y estado de cada plato.
// Los permisos por rol están en SecurityConfig.
@RestController
@RequestMapping("/table-orders")
@CrossOrigin(origins = "http://localhost:4200")
public class TableOrderController {

    @Autowired
    private TableOrderService service;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // ---------- Mesero ----------

    @Operation(summary = "Tables of a floor with active orders and upcoming reservations (waiter)")
    @GetMapping("/tables")
    public List<WaiterTableDTO> tables(@RequestParam Integer floor) {
        authenticatedUser.waiter();
        return service.tablesByFloor(floor);
    }

    @Operation(summary = "Active orders of a table (waiter)")
    @GetMapping("/tables/{tableId}")
    public List<TableOrderDTO> tableOrders(@PathVariable Integer tableId) {
        authenticatedUser.waiter();
        return service.activeOrdersOfTable(tableId);
    }

    @Operation(summary = "Register a table order for the logged-in waiter")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TableOrderDTO create(@RequestBody NewTableOrderDTO request) {
        return service.create(authenticatedUser.waiter().getId(), request);
    }

    @Operation(summary = "Deliver a ready order: DONE and the table attended (waiter)")
    @PostMapping("/{id}/deliver")
    public TableOrderDTO deliver(@PathVariable Integer id) {
        authenticatedUser.waiter();
        return service.deliver(id);
    }

    @Operation(summary = "Charge a delivered order: cash or card (waiter)")
    @PostMapping("/{id}/pay")
    public TableOrderDTO pay(@PathVariable Integer id,
            @RequestBody com.example.demo.dto.TableOrderDTOs.PaymentDTO request) {
        authenticatedUser.waiter();
        return service.pay(id, request == null ? null : request.method());
    }

    @Operation(summary = "Charge every delivered, unpaid order of a table at once (waiter)")
    @PostMapping("/tables/{tableId}/pay")
    public List<TableOrderDTO> payTable(@PathVariable Integer tableId,
            @RequestBody com.example.demo.dto.TableOrderDTOs.PaymentDTO request) {
        authenticatedUser.waiter();
        return service.payTable(tableId, request == null ? null : request.method());
    }

    @Operation(summary = "Cancel an order that the kitchen has not started (waiter)")
    @PostMapping("/{id}/cancel")
    public TableOrderDTO cancel(@PathVariable Integer id) {
        authenticatedUser.waiter();
        return service.cancel(id);
    }

    // ---------- Bandeja del mesero ----------

    @Operation(summary = "Kitchen notices for the logged-in waiter (latest 50)")
    @GetMapping("/notifications")
    public List<com.example.demo.dto.TableOrderDTOs.WaiterNotificationDTO> notifications() {
        return service.notifications(authenticatedUser.waiter().getId());
    }

    @Operation(summary = "Mark one notice as read (waiter)")
    @PutMapping("/notifications/{id}/read")
    public void markRead(@PathVariable Integer id) {
        service.markNotificationRead(authenticatedUser.waiter().getId(), id);
    }

    @Operation(summary = "Mark all notices as read (waiter)")
    @PutMapping("/notifications/read-all")
    public void markAllRead() {
        service.markAllNotificationsRead(authenticatedUser.waiter().getId());
    }

    // ---------- Cocina ----------

    @Operation(summary = "Active table orders grouped by table, oldest first (kitchen)")
    @GetMapping("/kitchen")
    public List<KitchenTableDTO> kitchen() {
        authenticatedUser.kitchen();
        return service.kitchen();
    }

    @Operation(summary = "Move a dish forward: PENDING -> PREPARING -> READY (kitchen)")
    @PutMapping("/{id}/items/{itemId}/status")
    public TableOrderDTO updateItem(@PathVariable Integer id, @PathVariable Integer itemId,
            @RequestBody ItemStatusDTO request) {
        authenticatedUser.kitchen();
        return service.updateItemStatus(id, itemId, request == null ? null : request.status());
    }
}
