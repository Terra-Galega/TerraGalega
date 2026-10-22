package com.example.demo.service;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.dto.OrderSummaryDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.Client;
import com.example.demo.dto.OrderDTO;
import java.util.List;

public interface OrderService {

    List<Order> getOrdersByClientId(Integer clientId);

    List<Order> getAllOrders();

    void clearClientIdFromOrders(Integer clientId);

    OrderDTO toDTO(Order order);

    Order getOrderById(Integer id);

    Order createOrder(Order order);

    Order checkout(Client client, CheckoutRequestDTO request);

    List<OrderSummaryDTO> getActiveOrders();

    List<OrderSummaryDTO> getAllOrderSummaries();

    Order confirmOrder(Integer orderId);

    Order cookOrder(Integer orderId, Integer operatorId);

    Order sendOrder(Integer orderId, Integer deliveryPersonId);

    Order deliverOrder(Integer orderId);

}