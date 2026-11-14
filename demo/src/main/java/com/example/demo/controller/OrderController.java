package com.example.demo.controller;

import java.util.Collection;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.OrderService;
import com.example.demo.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;

// API REST de pedidos. Ruta: /orders.

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    @Autowired
    private OrderService orderService;

    // Cancela pedidos pagados en línea (con reembolso) y también en efectivo
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // GET /orders
    @Operation(summary = "Get all orders for admin UI")
    @GetMapping
    public Collection<OrderDTO> getAllOrders() {
        return orderService.getAllOrders()
                .stream()
                .map(orderService::toDTO)
                .toList();
    }

    // GET /orders/me: historial del cliente con sesión iniciada
    @Operation(summary = "Get the orders of the logged-in client")
    @GetMapping("/me")
    public Collection<OrderDTO> getMyOrders() {
        return orderService.getOrdersByClientId(authenticatedUser.client().getId())
                .stream()
                .map(orderService::toDTO)
                .toList();
    }

    // GET /orders/{id}: detalle de un pedido, incluye cliente y domiciliario
    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable Integer id) {
        return orderService.toDTOWithContact(orderService.getOrderById(id));
    }

    // GET /orders/active: pedidos sin terminar (ni entregados ni cancelados)
    @Operation(summary = "Get active orders for operator UI")
    @GetMapping("/active")
    public Collection<OrderSummaryDTO> getActiveOrders() {
        return orderService.getActiveOrders();
    }

    // PUT /orders/{id}/confirm: PENDING pasa a CONFIRMED
    @Operation(summary = "Confirm a pending order")
    @PutMapping("/{id}/confirm")
    public OrderDTO confirmOrder(@PathVariable Integer id) {
        return orderService.toDTO(orderService.confirmOrder(id));
    }

    // PUT /orders/{id}/cook: CONFIRMED pasa a PREPARING.
    // El pedido queda a cargo del operador que hace la petición.
    @Operation(summary = "Start preparing an order (logged-in operator)")
    @PutMapping("/{id}/cook")
    public OrderDTO cookOrder(@PathVariable Integer id) {
        return orderService.toDTO(
                orderService.cookOrder(id, authenticatedUser.operator().getId()));
    }

    // PUT /orders/{id}/send/{deliveryPersonId}: PREPARING pasa a ON_THE_WAY
    @Operation(summary = "Send an order with a delivery person")
    @PutMapping("/{id}/send/{deliveryPersonId}")
    public OrderDTO sendOrder(
            @PathVariable Integer id,
            @PathVariable Integer deliveryPersonId) {
        return orderService.toDTO(
                orderService.sendOrder(id, deliveryPersonId));
    }

    // PUT /orders/{id}/deliver: ON_THE_WAY pasa a DELIVERED
    @Operation(summary = "Mark an order as delivered")
    @PutMapping("/{id}/deliver")
    public OrderDTO deliverOrder(@PathVariable Integer id) {
        return orderService.toDTO(
                orderService.deliverOrder(id));
    }

    // PUT /orders/{id}/cancel: PENDING pasa a CANCELLED.
    // El servicio comprueba que el pedido sea del cliente que hace la petición.
    @Operation(summary = "Cancel a pending order of the logged-in client")
    @PutMapping("/{id}/cancel")
    public OrderDTO cancelOrder(@PathVariable Integer id) {
        paymentService.cancel(id, authenticatedUser.client().getId());

        return orderService.toDTO(orderService.getOrderById(id));
    }

    // GET /orders/all?from=&to=&productId=&orderId=: resúmenes para el admin.
    @Operation(summary = "Get order summaries for admin UI, with optional filters")
    @GetMapping("/all")
    public Collection<OrderSummaryDTO> getAllOrderSummaries(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer productId,
            @RequestParam(required = false) Integer orderId) {
        return orderService.searchOrderSummaries(from, to, productId, orderId);
    }

}