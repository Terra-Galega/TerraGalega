package com.example.demo.whatsapp;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// Bandeja persistente de eventos autenticados. No ejecuta acciones del cliente.
@Entity
@Table(name = "whatsapp_webhook_events")
public class WhatsAppWebhookEvent {
    @Id
    @Column(length = 64)
    private String id;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;

    protected WhatsAppWebhookEvent() { }

    public WhatsAppWebhookEvent(String id, String payload) {
        this.id = id;
        this.payload = payload;
        this.receivedAt = Instant.now();
    }
}
