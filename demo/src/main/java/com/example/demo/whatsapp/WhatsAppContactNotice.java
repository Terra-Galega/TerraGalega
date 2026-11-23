package com.example.demo.whatsapp;

import java.time.Instant;
import jakarta.persistence.*;

/** Historial de avisos: atender una solicitud no elimina su notificación. */
@Entity
@Table(name = "whatsapp_contact_notices")
public class WhatsAppContactNotice {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    @Column(name = "source_message_id", nullable = false, unique = true, length = 255) String sourceMessageId;
    @Column(nullable = false, length = 30) String phone;
    @Column(name = "requested_at", nullable = false) Instant requestedAt;
    @Column(name = "is_read", nullable = false) boolean read;
    @Column(nullable = false) boolean handled;

    protected WhatsAppContactNotice() { }
    WhatsAppContactNotice(WhatsAppContactRequest request, String sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
        this.phone = request.phone;
        this.requestedAt = request.requestedAt;
        this.handled = !request.pending;
        this.read = this.handled;
    }
}
