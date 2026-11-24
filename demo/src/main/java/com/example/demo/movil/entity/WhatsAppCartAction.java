package com.example.demo.movil.entity;

import java.time.Instant;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Propuesta y resultado en la MISMA base/transacción que cart_lines. */
@Entity
@Table(name = "whatsapp_cart_actions", indexes = @Index(name = "idx_wa_cart_owner", columnList = "phone,client_id,state"))
@Getter @Setter
public class WhatsAppCartAction {
    @Id @Column(length = 64) private String id;
    @Column(nullable = false, length = 30) private String phone;
    @Column(name = "client_id", nullable = false) private Integer clientId;
    @Column(name = "linked_at", nullable = false) private Instant linkedAt;
    @Column(nullable = false, length = 20) private String state;
    @Column(nullable = false, length = 12) private String operation;
    @Column(name = "product_id") private Integer productId;
    @Column(name = "line_id", length = 200) private String lineId;
    private int quantity;
    @Column(name = "cart_hash", nullable = false, length = 64) private String cartHash;
    @Column(name = "unit_price") private Double unitPrice;
    @Column(nullable = false, length = 900) private String description;
    @Column(columnDefinition = "text") private String result;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "expires_at", nullable = false) private Instant expiresAt;
}
