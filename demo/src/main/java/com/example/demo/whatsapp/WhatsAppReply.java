package com.example.demo.whatsapp;

import java.time.Instant;
import jakarta.persistence.*;

@Entity
@Table(name = "whatsapp_replies", indexes = @Index(name = "idx_whatsapp_reply_due", columnList = "state,next_attempt_at"))
public class WhatsAppReply {
    @Id @Column(length = 255) String id;
    @Column(nullable = false, length = 30) String recipient;
    @Column(nullable = false, length = 30) String state;
    @Column(nullable = false) int attempts;
    @Column(name = "next_attempt_at", nullable = false) Instant nextAttemptAt;
    @Column(name = "expires_at", nullable = false) Instant expiresAt;
    @Column(name = "outbound_message_id", length = 255) String outboundMessageId;
    @Column(name = "last_error", length = 80) String lastError;
    // Nullable para las tareas que ya existían antes de conectar Gemini.
    @Column(name = "incoming_text", columnDefinition = "text") String incomingText;
    @Column(name = "reply_text", columnDefinition = "text") String replyText;

    protected WhatsAppReply() { }
    WhatsAppReply(String id, String recipient, Instant expiresAt) {
        this.id = id;
        this.recipient = recipient;
        this.expiresAt = expiresAt;
        this.nextAttemptAt = Instant.now();
        this.state = "PENDING";
    }
}
