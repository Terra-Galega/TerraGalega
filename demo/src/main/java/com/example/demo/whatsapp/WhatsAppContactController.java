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
            return ResponseEntity.noContent().build();
        });
    }
}
