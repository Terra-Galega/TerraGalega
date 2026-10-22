package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.demo.entities.OrderStatus;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class OrderDTO {
    private Integer id;
    private ClientOrderDTO client;
    private DeliveryPersonOrderDTO deliveryPerson;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private String address;
    private List<OrderDetailDTO> details;
    private Double total;
}