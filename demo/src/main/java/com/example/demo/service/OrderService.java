package com.example.demo.service;

import com.example.demo.entities.Order;

import java.util.List;

public interface OrderService {

    List<Order> getOrdersByClientId(Integer clientId);

    Order getOrderById(Integer id);

    Order createOrder(Order order);

}