package com.example.demo.whatsapp;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Borra cada noche los datos de WhatsApp que ya no se necesitan. Los plazos se
 * publican en la política de privacidad; si cambian aquí, actualízala también.
 */
@Service
public class WhatsAppRetention {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppRetention.class);

    @PersistenceContext(unitName = "h2") private EntityManager em;
    @PersistenceContext(unitName = "postgres") private EntityManager pg;
    private final TransactionTemplate tx;
    private final TransactionTemplate pgTx;
    private final int conversationDays;

    public WhatsAppRetention(PlatformTransactionManager transactionManager,
            @Qualifier("postgresTransactionManager") PlatformTransactionManager postgresManager,
            @Value("${terra.whatsapp.retention-days:30}") int conversationDays) {
        this.tx = new TransactionTemplate(transactionManager);
        this.pgTx = new TransactionTemplate(postgresManager);
        this.conversationDays = conversationDays;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Bogota")
    public void purge() {
        Instant now = Instant.now();
        Instant conversations = now.minus(Duration.ofDays(conversationDays));
        Instant proposals = now.minus(Duration.ofDays(7));
        Instant notices = now.minus(Duration.ofDays(90));
        try {
            int[] h2 = tx.execute(status -> new int[] {
                // Cuerpos completos de Meta: nombre, teléfono y texto del cliente.
                em.createQuery("delete from WhatsAppWebhookEvent e where e.receivedAt < :t").setParameter("t", conversations).executeUpdate(),
                // Mensajes y respuestas, que también sirven de historial para Gemini.
                em.createQuery("delete from WhatsAppReply r where r.expiresAt < :t").setParameter("t", conversations).executeUpdate(),
                em.createQuery("delete from ReservationProposal p where p.expiresAt < :t").setParameter("t", proposals).executeUpdate(),
                em.createQuery("delete from WhatsAppContactNotice n where n.handled = true and n.requestedAt < :t").setParameter("t", notices).executeUpdate(),
                // Enlaces de vinculación que nunca se usaron.
                em.createQuery("delete from WhatsAppAccountLink l where l.linkedAt is null and l.expiresAt < :t").setParameter("t", now).executeUpdate(),
            });
            int carts = pgTx.execute(status -> pg.createQuery("delete from WhatsAppCartAction a where a.expiresAt < :t")
                    .setParameter("t", proposals).executeUpdate());
            log.info("Limpieza de WhatsApp: eventos={}, mensajes={}, reservas={}, avisos={}, enlaces={}, carrito={}",
                    h2[0], h2[1], h2[2], h2[3], h2[4], carts);
        } catch (RuntimeException failure) {
            log.warn("No se pudo completar la limpieza de WhatsApp ({})", failure.getClass().getSimpleName());
        }
    }
}
