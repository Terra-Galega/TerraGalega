package com.example.demo.whatsapp;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.RestClientResponseException;
import tools.jackson.databind.ObjectMapper;

@Service
public class WhatsAppReplyService {
    private static final Logger log = LoggerFactory.getLogger(WhatsAppReplyService.class);
    private final WhatsAppReplySettings settings;
    private final WhatsAppMessageClient client;
    private final ObjectMapper mapper;
    private final EntityManager em;
    private final TransactionTemplate tx;
    private final WhatsAppAssistant assistant;
    private final WhatsAppMenu menu;

    public WhatsAppReplyService(WhatsAppReplySettings settings, WhatsAppMessageClient client, ObjectMapper mapper,
            @Qualifier("entityManagerFactory") EntityManagerFactory factory,
            @Qualifier("transactionManager") PlatformTransactionManager manager, WhatsAppAssistant assistant, WhatsAppMenu menu) {
        this.settings = settings;
        this.client = client;
        this.mapper = mapper;
        this.em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        this.tx = new TransactionTemplate(manager);
        this.assistant = assistant;
        this.menu = menu;
    }

    // Participa en la misma transacción que guarda el webhook: no pierde trabajos
    // si Railway reinicia justo después de confirmar la recepción a Meta.
    void enqueue(EntityManager entityManager, byte[] body) {
        if (!settings.enabled()) return;
        var root = mapper.readTree(body);
        Instant now = Instant.now();
        for (var entry : root.path("entry")) {
            for (var change : entry.path("changes")) {
                var value = change.path("value");
                if (!"messages".equals(change.path("field").asText())
                        || !settings.phoneNumberId().equals(value.path("metadata").path("phone_number_id").asText())) continue;
                for (var message : value.path("messages")) {
                    String id = message.path("id").asText();
                    String from = message.path("from").asText();
                    var selection = menu.selection(message);
                    if (selection == null || id.isBlank() || id.length() > 255 || !from.matches("[0-9]{5,25}")) continue;
                    Instant sentAt;
                    try { sentAt = Instant.ofEpochSecond(Long.parseLong(message.path("timestamp").asText())); }
                    catch (RuntimeException invalidTimestamp) { continue; }
                    // Margen antes de vencer la ventana de 24h. Descarta ejemplos
                    // históricos del panel Meta y fechas futuras inconsistentes.
                    Instant expires = sentAt.plus(Duration.ofHours(23));
                    if (!expires.isAfter(now) || sentAt.isAfter(now.plusSeconds(300))) continue;
                    if (entityManager.find(WhatsAppReply.class, id) == null) {
                        var job = new WhatsAppReply(id, from, expires);
                        String text = selection.text();
                        job.responseKind = selection.kind();
                        // Conserva longitud suficiente para contestar que excede el límite,
                        // sin duplicar un cuerpo arbitrariamente grande en la cola.
                        job.incomingText = text.substring(0, Math.min(text.length(), WhatsAppAssistant.MAX_INPUT + 1));
                        entityManager.persist(job);
                        if ("OPERATOR".equals(job.responseKind)) {
                            var request = entityManager.find(WhatsAppContactRequest.class, from, LockModeType.PESSIMISTIC_WRITE);
                            if (request == null) entityManager.persist(new WhatsAppContactRequest(from));
                            else if (!request.pending) {
                                request.pending = true;
                                request.requestedAt = Instant.now();
                            }
                        }
                    }
                }
            }
        }
    }

    @Scheduled(fixedDelay = 3000, initialDelay = 10000, scheduler = "whatsappReplyScheduler")
    public void dispatch() {
        if (!settings.enabled()) return;
        // Lotes pequeños: el webhook nunca espera una llamada a Graph API.
        for (int i = 0; i < 5; i++) {
            WhatsAppReply job = tx.execute(status -> claim());
            if (job == null) return;
            String state;
            String error = null;
            String outbound = null;
            try {
                if (job.replyText == null) {
                    List<WhatsAppAssistant.Turn> history = tx.execute(status -> history(job));
                    String fixed = menu.fixedAnswer(job.responseKind == null ? "AI" : job.responseKind);
                    String generated = fixed != null ? fixed : assistant.answer(job.incomingText, history);
                    tx.executeWithoutResult(status -> em.find(WhatsAppReply.class, job.id).replyText = generated);
                    job.replyText = generated;
                }
                // Gemini puede tardar; comprobar de nuevo la ventana antes del envío.
                if (!job.expiresAt.isAfter(Instant.now())) {
                    tx.executeWithoutResult(status -> em.find(WhatsAppReply.class, job.id).state = "EXPIRED");
                    continue;
                }
                outbound = "MENU".equals(job.responseKind) ? client.sendMenu(job.recipient, job.replyText)
                        : client.send(job.recipient, job.replyText);
                state = "ACCEPTED";
            } catch (RestClientResponseException failure) {
                int code = failure.getStatusCode().value();
                // Solo reintentamos un rechazo explícito por exceso de peticiones.
                state = code == 429 && job.attempts < 3 ? "PENDING" : "FAILED";
                error = "HTTP_" + code;
                if (code >= 500) state = "UNKNOWN";
            } catch (RuntimeException ambiguous) {
                // Ante timeout puede haberse enviado. No duplicar automáticamente.
                state = "UNKNOWN";
                error = "RESULTADO_NO_CONFIRMADO";
            }
            final String resultState = state, resultError = error, resultOutbound = outbound;
            tx.executeWithoutResult(status -> {
                var stored = em.find(WhatsAppReply.class, job.id, LockModeType.PESSIMISTIC_WRITE);
                stored.state = resultState;
                stored.lastError = resultError;
                stored.outboundMessageId = resultOutbound;
                stored.nextAttemptAt = Instant.now().plusSeconds(60L * stored.attempts);
            });
            log.info("Respuesta de WhatsApp: estado={}, error={}", state, error);
        }
    }

    private List<WhatsAppAssistant.Turn> history(WhatsAppReply job) {
        var previous = em.createQuery("select r from WhatsAppReply r where r.recipient = :recipient and r.state = 'ACCEPTED' and r.expiresAt > :now and r.expiresAt <= :current and r.id <> :id and r.incomingText is not null and r.replyText is not null order by r.expiresAt desc, r.id desc", WhatsAppReply.class)
                .setParameter("recipient", job.recipient).setParameter("now", Instant.now())
                .setParameter("current", job.expiresAt).setParameter("id", job.id).setMaxResults(6).getResultList();
        var turns = new ArrayList<WhatsAppAssistant.Turn>();
        for (var item : previous) turns.add(new WhatsAppAssistant.Turn(item.incomingText, item.replyText));
        Collections.reverse(turns);
        return turns;
    }

    private WhatsAppReply claim() {
        var candidates = em.createQuery("select r from WhatsAppReply r where r.state = 'PENDING' and r.nextAttemptAt <= :now order by r.nextAttemptAt", WhatsAppReply.class)
                .setParameter("now", Instant.now()).setMaxResults(1).getResultList();
        if (candidates.isEmpty()) return null;
        var job = em.find(WhatsAppReply.class, candidates.getFirst().id, LockModeType.PESSIMISTIC_WRITE);
        em.refresh(job);
        if (!"PENDING".equals(job.state) || job.nextAttemptAt.isAfter(Instant.now())) return null;
        if (!job.expiresAt.isAfter(Instant.now())) { job.state = "EXPIRED"; return null; }
        job.state = "SENDING";
        job.attempts++;
        return job;
    }
}
