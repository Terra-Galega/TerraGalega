package com.example.demo.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class WhatsAppWebhookInbox {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookInbox.class);
    private final EntityManager entityManager;
    private final TransactionTemplate transaction;
    private final WhatsAppReplyService replies;

    public WhatsAppWebhookInbox(@Qualifier("entityManagerFactory") EntityManagerFactory factory,
            @Qualifier("transactionManager") PlatformTransactionManager manager, WhatsAppReplyService replies) {
        this.entityManager = SharedEntityManagerCreator.createSharedEntityManager(factory);
        this.transaction = new TransactionTemplate(manager);
        this.replies = replies;
    }

    public void receive(byte[] body) {
        String id = digest(body);
        try {
            transaction.executeWithoutResult(status -> {
                if (entityManager.find(WhatsAppWebhookEvent.class, id) == null) {
                    entityManager.persist(new WhatsAppWebhookEvent(id, new String(body, StandardCharsets.UTF_8)));
                    replies.enqueue(entityManager, body);
                    entityManager.flush();
                }
            });
        } catch (RuntimeException failure) {
            // Dos instancias pueden recibir la misma entrega a la vez. Solo se
            // confirma si una transacción ya guardó el evento completo.
            Boolean stored = transaction.execute(status -> entityManager.find(WhatsAppWebhookEvent.class, id) != null);
            if (!Boolean.TRUE.equals(stored)) {
                throw failure;
            }
        }
        // No se registran teléfonos, contenido de mensajes ni secretos en logs.
        log.info("Evento de WhatsApp recibido en bandeja: {}", id);
    }

    private String digest(byte[] body) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(body));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
