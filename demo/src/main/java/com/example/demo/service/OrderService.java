package com.example.demo.service;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.Client;

import java.util.List;

public interface OrderService {

    List<Order> getOrdersByClientId(Integer clientId);

    List<Order> getAllOrders();

    Order getOrderById(Integer id);

    Order createOrder(Order order);

    Order checkout(Client client, CheckoutRequestDTO request);

}