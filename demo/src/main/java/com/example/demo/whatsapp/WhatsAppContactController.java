package com.example.demo.whatsapp;

import java.time.Instant;
import java.util.List;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/operators/whatsapp-requests")
public class WhatsAppContactController {
    private final EntityManager em;
    private final TransactionTemplate tx;

    public record ContactRequest(String phone, Instant requestedAt) { }
    public record ContactNotice(Long id, String phone, Instant requestedAt, boolean read, boolean handled) { }

    @GetMapping("/notifications")
    public List<ContactNotice> notifications() {
        return tx.execute(status -> {
            // Recupera solicitudes creadas por la versión anterior, sin perderlas
            // al estrenar la campana. La clave única protege contra carreras.
            var legacy = em.createQuery("select r from WhatsAppContactRequest r where not exists (select n.id from WhatsAppContactNotice n where n.phone = r.phone and n.requestedAt = r.requestedAt)", WhatsAppContactRequest.class)
                    .setMaxResults(100).getResultList();
            for (var request : legacy) {
                em.persist(new WhatsAppContactNotice(request, "legacy:" + request.phone + ":" + request.requestedAt));
            }
            em.flush();
            return em.createQuery("select n from WhatsAppContactNotice n order by n.requestedAt desc, n.id desc", WhatsAppContactNotice.class)
                    .setMaxResults(100).getResultStream()
                    .map(n -> new ContactNotice(n.id, n.phone, n.requestedAt, n.read, n.handled)).toList();
        });
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<Void> markRead(@PathVariable Long id) {
        return tx.execute(status -> {
            var notice = em.find(WhatsAppContactNotice.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (notice == null) return ResponseEntity.notFound().build();
            notice.read = true;
            return ResponseEntity.noContent().build();
        });
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<Void> markAllRead() {
        tx.executeWithoutResult(status -> em.createQuery("update WhatsAppContactNotice n set n.read = true where n.read = false").executeUpdate());
        return ResponseEntity.noContent().build();
    }

    public WhatsAppContactController(@Qualifier("entityManagerFactory") EntityManagerFactory factory,
            @Qualifier("transactionManager") PlatformTransactionManager manager) {
        em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        tx = new TransactionTemplate(manager);
    }

    @GetMapping
    public List<ContactRequest> pending() {
        return tx.execute(status -> em.createQuery("select r from WhatsAppContactRequest r where r.pending = true order by r.requestedAt desc", WhatsAppContactRequest.class)
                .setMaxResults(100).getResultStream().map(r -> new ContactRequest(r.phone, r.requestedAt)).toList());
    }

    @PutMapping("/{phone}/handled")
    public ResponseEntity<Void> handled(@PathVariable String phone, @RequestParam Instant requestedAt) {
        return tx.execute(status -> {
            var request = em.find(WhatsAppContactRequest.class, phone, LockModeType.PESSIMISTIC_WRITE);
            if (request == null) return ResponseEntity.notFound().build();
            // Una confirmación vieja no debe ocultar una solicitud nueva.
            if (!request.requestedAt.equals(requestedAt)) return ResponseEntity.status(409).build();
            request.pending = false;
            em.createQuery("update WhatsAppContactNotice n set n.handled = true, n.read = true where n.phone = :phone and n.requestedAt = :time")
                    .setParameter("phone", phone).setParameter("time", requestedAt).executeUpdate();
            return ResponseEntity.noContent().build();
        });
    }
}
