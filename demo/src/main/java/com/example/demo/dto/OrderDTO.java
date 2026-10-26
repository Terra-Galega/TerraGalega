package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.example.demo.entities.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
public class OrderDTO {
    private Integer id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ClientOrderDTO client;
    @JsonInclude(JsonInclude.Include.NON_NULL)
private DeliveryPersonOrderDTO deliveryPerson;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private String address;
    private List<OrderDetailDTO> details;
    private Double total;
}