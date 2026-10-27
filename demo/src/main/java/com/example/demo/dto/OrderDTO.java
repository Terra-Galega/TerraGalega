package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.example.demo.entities.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

/**
 * Representación de un pedido que se envía al frontend. Se usa en lugar de la
 * entidad Order para exponer solo lo necesario.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderDTO {
    private Integer id;
    // NON_NULL: si no se cargó el cliente (toDTO sin contacto) el campo ni aparece en el JSON
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ClientOrderDTO client;
    // Igual que el cliente: solo aparece cuando se pidió el pedido con contacto y ya tiene un domiciliario asignado
    @JsonInclude(JsonInclude.Include.NON_NULL)
private DeliveryPersonOrderDTO deliveryPerson;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime deliveredAt;
    private String address;
    private List<OrderDetailDTO> details;
    // Total calculado en la entidad: productos + adicionales + domicilio
    private Double total;
}