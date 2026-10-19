package com.example.demo.controller;

import java.util.Collection;

import com.example.demo.entities.Order;
import com.example.demo.dto.OrderSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation(summary = "Get all orders for admin UI")
    @GetMapping()
    public Collection<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @Operation(summary = "Get all orders for a specific client")
    @GetMapping("/client/{clientId}")
    public Collection<Order> getOrdersByClient(@PathVariable Integer clientId) {
        return orderService.getOrdersByClientId(clientId);
    }

    @Operation(summary = "Get order by ID for admin UI")
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Integer id) {
        return orderService.getOrderById(id);
    }

    @Operation(summary = "Get active orders (not delivered or cancelled) for operator UI")
    @GetMapping("/active")
    public Collection<OrderSummaryDTO> getActiveOrders() {
        return orderService.getActiveOrders();
    }

    @Operation(summary = "Get all orders (flat summary) for admin UI")
    @GetMapping("/all")
    public Collection<OrderSummaryDTO> getAllOrderSummaries() {
        return orderService.getAllOrderSummaries();
    }

    @Operation(summary = "Operator confirms a pending order (PENDING -> CONFIRMED)")
    @PutMapping("/{id}/confirm")
    public Order confirmOrder(@PathVariable Integer id) {
        return orderService.confirmOrder(id);
    }

    @Operation(summary = "Operator starts cooking a pending order (PENDING -> PREPARING)")
    @PutMapping("/{id}/cook/{operatorId}")
    public Order cookOrder(@PathVariable Integer id, @PathVariable Integer operatorId) {
        return orderService.cookOrder(id, operatorId);
    }

    @Operation(summary = "Operator sends an order with a delivery person (PREPARING -> ON_THE_WAY)")
    @PutMapping("/{id}/send/{deliveryPersonId}")
    public Order sendOrder(@PathVariable Integer id, @PathVariable Integer deliveryPersonId) {
        return orderService.sendOrder(id, deliveryPersonId);
    }

}