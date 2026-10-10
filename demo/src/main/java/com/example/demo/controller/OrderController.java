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

@RestController
@RequestMapping("/orders")
@CrossOrigin(origins = "http://localhost:4200")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping()
    public Collection<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/client/{clientId}")
    public Collection<Order> getOrdersByClient(@PathVariable Integer clientId) {
        return orderService.getOrdersByClientId(clientId);
    }

    @GetMapping("/{id}")
    public Order getOrderById(@PathVariable Integer id) {
        return orderService.getOrderById(id);
    }

}