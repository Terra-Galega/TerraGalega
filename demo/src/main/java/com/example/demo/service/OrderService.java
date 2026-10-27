package com.example.demo.service;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.dto.OrderSummaryDTO;
import com.example.demo.entities.Order;
import com.example.demo.entities.Client;
import com.example.demo.dto.OrderDTO;
import java.util.List;
import java.time.LocalDate;

public interface OrderService {

    Order cancelOrder(Integer orderId, Integer clientId);

    List<Order> getOrdersByClientId(Integer clientId);

    List<Order> getAllOrders();

    List<OrderSummaryDTO> searchOrderSummaries(LocalDate from, LocalDate to, Integer productId, Integer orderId);

    void clearClientIdFromOrders(Integer clientId);

    void cancelPendingOrdersForClientDeletion(Integer clientId);

    OrderDTO toDTO(Order order);

    OrderDTO toDTOWithContact(Order order);

    Order getOrderById(Integer id);

    Order createOrder(Order order);

    Order checkout(Client client, CheckoutRequestDTO request);

    List<OrderSummaryDTO> getActiveOrders();

    List<OrderSummaryDTO> getAllOrderSummaries();

    // PENDING -> CONFIRMED
    Order confirmOrder(Integer orderId);

    // CONFIRMED -> PREPARING
    Order cookOrder(Integer orderId, Integer operatorId);

    // PREPARING -> ON_THE_WAY
    Order sendOrder(Integer orderId, Integer deliveryPersonId);

    // ON_THE_WAY -> DELIVERED
    Order deliverOrder(Integer orderId);

}