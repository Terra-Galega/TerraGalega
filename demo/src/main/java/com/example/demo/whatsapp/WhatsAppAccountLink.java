package com.example.demo.whatsapp;

import java.time.Instant;
import jakarta.persistence.*;

/** El teléfono de WhatsApp verificado es independiente del teléfono de contacto del perfil. */
@Entity
@Table(name = "whatsapp_account_links")
public class WhatsAppAccountLink {
    @Id @Column(length = 30) String phone;
    @Column(name = "client_id", unique = true) Integer clientId;
    @Column(name = "token_hash", unique = true, length = 64) String tokenHash;
    @Column(name = "expires_at") Instant expiresAt;
    @Column(name = "linked_at") Instant linkedAt;

    protected WhatsAppAccountLink() { }
    WhatsAppAccountLink(String phone) { this.phone = phone; }
}
