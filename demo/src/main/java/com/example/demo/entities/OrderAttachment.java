package com.example.demo.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Foto que el cliente envía por el chat del pedido. Se guarda en la base (BLOB),
// en una tabla aparte para que la lista de mensajes no cargue las imágenes.
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "order_attachments")
public class OrderAttachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false)
    private Integer orderId;

    // "image/jpeg" o "image/png" (lo decide el back leyendo los bytes)
    @Column(nullable = false, length = 20)
    private String contentType;

    // Tamaño en bytes
    @Column(nullable = false)
    private Integer size;

    @Lob
    @Column(nullable = false)
    private byte[] data;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}