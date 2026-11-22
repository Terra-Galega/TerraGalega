package com.example.demo.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Mensaje del chat entre el cliente y el operador sobre un pedido.
// Guarda el id del pedido sin relación JPA
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_messages")
public class OrderMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer orderId;

    // "CLIENT" u "OPERATOR"
    @Column(nullable = false, length = 10)
    private String senderRole;

    @Column(nullable = false, length = 100)
    private String senderName;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}