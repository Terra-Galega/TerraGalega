package com.example.demo.controller;

import java.util.Collection;

import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

import com.example.demo.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "Get all orders for admin UI")
    @GetMapping
    public Collection<OrderDTO> getAllOrders() {
        return orderService.getAllOrders()
                .stream()
                .map(orderService::toDTO)
                .toList();
    }

    @Operation(summary = "Get all orders for a specific client")
    @GetMapping("/client/{clientId}")
    public Collection<OrderDTO> getOrdersByClient(
            @PathVariable Integer clientId) {
        return orderService.getOrdersByClientId(clientId)
                .stream()
                .map(orderService::toDTO)
                .toList();
    }

    @Operation(summary = "Get order by ID")
    @GetMapping("/{id}")
    public OrderDTO getOrderById(@PathVariable Integer id) {
        return orderService.toDTO(orderService.getOrderById(id));
    }

    @Operation(summary = "Get active orders for operator UI")
    @GetMapping("/active")
    public Collection<OrderSummaryDTO> getActiveOrders() {
        return orderService.getActiveOrders();
    }

    @Operation(summary = "Confirm a pending order")
    @PutMapping("/{id}/confirm")
    public OrderDTO confirmOrder(@PathVariable Integer id) {
        return orderService.toDTO(orderService.confirmOrder(id));
    }

    @Operation(summary = "Start preparing an order")
    @PutMapping("/{id}/cook/{operatorId}")
    public OrderDTO cookOrder(
            @PathVariable Integer id,
            @PathVariable Integer operatorId) {
        return orderService.toDTO(
                orderService.cookOrder(id, operatorId));
    }

    @Operation(summary = "Send an order with a delivery person")
    @PutMapping("/{id}/send/{deliveryPersonId}")
    public OrderDTO sendOrder(
            @PathVariable Integer id,
            @PathVariable Integer deliveryPersonId) {
        return orderService.toDTO(
                orderService.sendOrder(id, deliveryPersonId));
    }

    @Operation(summary = "Mark an order as delivered")
    @PutMapping("/{id}/deliver")
    public OrderDTO deliverOrder(@PathVariable Integer id) {
        return orderService.toDTO(
                orderService.deliverOrder(id));
    }

    @Operation(summary = "Cancel a pending order (only by its client)")
    @PutMapping("/{id}/cancel")
    public OrderDTO cancelOrder(
            @PathVariable Integer id,
            @RequestParam Integer clientId) {
        return orderService.toDTO(orderService.cancelOrder(id, clientId));
    }

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