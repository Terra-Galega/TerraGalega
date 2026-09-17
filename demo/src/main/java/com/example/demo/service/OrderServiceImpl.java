package com.example.demo.service;

import com.example.demo.entities.Order;
import com.example.demo.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository repository;

    @Override
    @Transactional
    public List<Order> getOrdersByClientId(Integer clientId) {
        return repository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    @Transactional
    public Order getOrderById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe."));
    }

    @Override
    @Transactional
    public Order createOrder(Order order) {
        return repository.save(order);
    }

}