package com.example.demo.whatsapp;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import jakarta.persistence.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.demo.errors.ApiException;

@Service
public class WhatsAppAccountService {
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final SecureRandom random = new SecureRandom();
    private final String website;

    public WhatsAppAccountService(@Qualifier("entityManagerFactory") EntityManagerFactory factory,
            @Qualifier("transactionManager") PlatformTransactionManager manager,
            @Value("${terra.frontend-url:}") String website) {
        em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        tx = new TransactionTemplate(manager);
        this.website = safeWebsite(website);
    }

    public record Status(boolean linked, String phone, Instant linkedAt) { }
    public record Preview(String maskedPhone, Instant expiresAt) { }
    record Account(Integer clientId, Instant linkedAt) { }
    static final String LINK_REQUIRED = "Para gestionar tu carrito, pedidos o reservas, primero elige 🔗 Vincular mi cuenta en el menú. Si ya lo vinculaste, solicita de nuevo la acción desde ese WhatsApp.";

    Account snapshot(EntityManager entityManager, String phone) {
        var link = entityManager.find(WhatsAppAccountLink.class, phone);
        return link == null || link.clientId == null ? null : new Account(link.clientId, link.linkedAt);
    }

    boolean matches(String phone, Account account) {
        return account != null && Boolean.TRUE.equals(tx.execute(status -> account.equals(snapshot(em, phone))));
    }

    // La asociación se verifica al ejecutar cada herramienta, no la decide Gemini.
    <T> T withAccount(String phone, Account account, java.util.function.Supplier<T> work) {
        if (account == null) throw ApiException.forbidden(LINK_REQUIRED);
        return tx.execute(status -> {
            if (em.createNativeQuery("select id from clients where id = :client for update", Integer.class)
                    .setParameter("client", account.clientId()).getResultList().isEmpty()) throw ApiException.forbidden(LINK_REQUIRED);
            var link = em.find(WhatsAppAccountLink.class, phone, LockModeType.PESSIMISTIC_WRITE);
            if (link != null) em.refresh(link, LockModeType.PESSIMISTIC_WRITE);
            if (link == null || !account.clientId().equals(link.clientId) || !account.linkedAt().equals(link.linkedAt)) {
                throw ApiException.forbidden(LINK_REQUIRED);
            }
            return work.get();
        });
    }

    // Solo lo invoca el consumidor de mensajes entrantes firmados por Meta.
    String createReply(String phone) {
        if (website.isEmpty()) return "No podemos generar el enlace en este momento. Inténtalo más tarde o solicita un operador.";
        if (phone == null || !phone.matches("[0-9]{5,25}")) throw new IllegalArgumentException("Teléfono inválido");
        return tx.execute(transaction -> {
            var link = em.find(WhatsAppAccountLink.class, phone, LockModeType.PESSIMISTIC_WRITE);
            if (link == null) {
                link = new WhatsAppAccountLink(phone);
                em.persist(link);
            }
            if (link.clientId != null) return "Este WhatsApp ya está vinculado a una cuenta de TerraGalega. Puedes gestionar o quitar la vinculación desde Mi cuenta en la web.";
            byte[] bytes = new byte[32];
            random.nextBytes(bytes);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            link.tokenHash = hash(token);
            link.expiresAt = Instant.now().plusSeconds(15 * 60);
            return "🔗 Vincula tu cuenta de TerraGalega: " + website + "/whatsapp/link#token=" + token
                    + "\nInicia sesión y confirma el número. El enlace es personal, de un solo uso y vence en 15 minutos; no lo compartas. Pedir otro enlace invalida el anterior.";
        });
    }

    public Status status(Integer clientId) {
        return tx.execute(transaction -> current(clientId));
    }

    public Preview preview(String token) {
        return tx.execute(transaction -> {
            var link = pending(token, false);
            return new Preview("+" + link.phone.substring(0, Math.min(2, link.phone.length() - 4))
                    + " •••••• " + link.phone.substring(link.phone.length() - 4), link.expiresAt);
        });
    }

    public Status confirm(Integer clientId, String token) {
        try {
            return tx.execute(transaction -> {
                // Serializa también contra otra vinculación o el borrado de esta cuenta.
                // Proyección: bloquear la fila sin cargar el grafo de reservas/pedidos del cliente.
                if (em.createNativeQuery("select id from clients where id = :client for update", Integer.class)
                        .setParameter("client", clientId).getResultList().isEmpty()) {
                    throw ApiException.unauthorized("La cuenta ya no está disponible.");
                }
                var link = pending(token, true);
                var existing = current(clientId);
                if (existing.linked()) throw ApiException.conflict("Tu cuenta ya tiene un WhatsApp vinculado. Desvincúlalo antes de cambiarlo.");
                link.clientId = clientId;
                link.linkedAt = Instant.now();
                link.tokenHash = null;
                link.expiresAt = null;
                em.flush(); // UNIQUE también protege las confirmaciones concurrentes.
                return new Status(true, link.phone, link.linkedAt);
            });
        } catch (org.springframework.dao.DataIntegrityViolationException | org.hibernate.exception.ConstraintViolationException conflict) {
            throw ApiException.conflict("La cuenta o el número ya están vinculados. Actualiza la página y revisa Mi cuenta.");
        }
    }

    public void unlink(Integer clientId) {
        tx.executeWithoutResult(transaction -> {
            var links = em.createQuery("select l from WhatsAppAccountLink l where l.clientId = :client", WhatsAppAccountLink.class)
                    .setParameter("client", clientId).setLockMode(LockModeType.PESSIMISTIC_WRITE).getResultList();
            for (var link : links) {
                em.refresh(link, LockModeType.PESSIMISTIC_WRITE);
                if (!clientId.equals(link.clientId)) continue;
                link.clientId = null;
                link.linkedAt = null;
                link.tokenHash = null;
                link.expiresAt = null;
            }
        });
    }

    private Status current(Integer clientId) {
        return em.createQuery("select l from WhatsAppAccountLink l where l.clientId = :client", WhatsAppAccountLink.class)
                .setParameter("client", clientId).getResultStream()
                .map(l -> new Status(true, l.phone, l.linkedAt)).findFirst().orElse(new Status(false, null, null));
    }

    private WhatsAppAccountLink pending(String token, boolean lock) {
        if (token == null || !token.matches("[A-Za-z0-9_-]{43}")) throw invalidLink();
        var query = em.createQuery("select l from WhatsAppAccountLink l where l.tokenHash = :hash", WhatsAppAccountLink.class)
                .setParameter("hash", hash(token));
        if (lock) query.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        var link = query.getResultStream().findFirst().orElseThrow(WhatsAppAccountService::invalidLink);
        // Releer tras esperar el bloqueo: un segundo consumidor no puede reutilizarlo.
        if (lock) em.refresh(link, LockModeType.PESSIMISTIC_WRITE);
        if (!hash(token).equals(link.tokenHash) || link.clientId != null || link.expiresAt == null || !link.expiresAt.isAfter(Instant.now())) throw invalidLink();
        return link;
    }

    private static ApiException invalidLink() {
        return ApiException.badRequest("El enlace venció, ya se usó o fue reemplazado. Pide uno nuevo a Galleguito con «Vincular mi cuenta».");
    }

    private static String hash(String token) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8))); }
        catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private static String safeWebsite(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            return "https".equals(uri.getScheme()) && uri.getHost() != null && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null && uri.getRawFragment() == null ? uri.toString().replaceAll("/+$", "") : "";
        } catch (IllegalArgumentException invalid) { return ""; }
    }
}
