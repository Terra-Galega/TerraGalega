package com.example.demo.controller;

import java.util.Collection;

import com.example.demo.entities.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.service.OrderService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @Operation (summary = "Get all orders for admin UI")
    @GetMapping()
    public Collection<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @Operation (summary = "Get all orders for a specific client")
    @GetMapping("/client/{clientId}")
    public Collection<Order> getOrdersByClient(@PathVariable Integer clientId) {
        return orderService.getOrdersByClientId(clientId);
    }

    @Operation (summary = "Get order by ID for admin UI")
    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Integer id) {
        return orderService.getOrderById(id);
    }

}