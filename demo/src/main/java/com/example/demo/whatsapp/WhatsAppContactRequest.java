package com.example.demo.whatsapp;

import java.time.Instant;
import jakarta.persistence.*;

/** Aviso compartido para operadores, sin exponer el historial del chat. */
@Entity
@Table(name = "whatsapp_contact_requests")
public class WhatsAppContactRequest {
    @Id @Column(length = 30) String phone;
    @Column(name = "requested_at", nullable = false) Instant requestedAt;
    @Column(nullable = false) boolean pending;

    protected WhatsAppContactRequest() { }
    WhatsAppContactRequest(String phone) {
        this.phone = phone;
        this.pending = true;
        this.requestedAt = Instant.now();
    }
}
