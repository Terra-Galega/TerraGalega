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
    private final WhatsAppAccountService accounts;
    private final WhatsAppCartService cart;
    private final WhatsAppCartAssistant cartAssistant;

    public WhatsAppReplyService(WhatsAppReplySettings settings, WhatsAppMessageClient client, ObjectMapper mapper,
            @Qualifier("entityManagerFactory") EntityManagerFactory factory,
            @Qualifier("transactionManager") PlatformTransactionManager manager, WhatsAppAssistant assistant, WhatsAppMenu menu,
            WhatsAppAccountService accounts, WhatsAppCartService cart, WhatsAppCartAssistant cartAssistant) {
        this.settings = settings;
        this.client = client;
        this.mapper = mapper;
        this.em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        this.tx = new TransactionTemplate(manager);
        this.assistant = assistant;
        this.menu = menu;
        this.accounts = accounts;
        this.cart = cart;
        this.cartAssistant = cartAssistant;
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
                        var account = accounts.snapshot(entityManager, from);
                        if (account != null) {
                            job.accountClientId = account.clientId();
                            job.accountLinkedAt = account.linkedAt();
                        }
                        String text = selection.text();
                        job.responseKind = selection.kind();
                        // Conserva longitud suficiente para contestar que excede el límite,
                        // sin duplicar un cuerpo arbitrariamente grande en la cola.
                        job.incomingText = text.substring(0, Math.min(text.length(), WhatsAppAssistant.MAX_INPUT + 1));
                        entityManager.persist(job);
                        if ("OPERATOR".equals(job.responseKind)) {
                            var request = entityManager.find(WhatsAppContactRequest.class, from, LockModeType.PESSIMISTIC_WRITE);
                            if (request == null) {
                                request = new WhatsAppContactRequest(from);
                                entityManager.persist(request);
                                entityManager.persist(new WhatsAppContactNotice(request, job.id));
                            } else if (!request.pending) {
                                request.pending = true;
                                request.requestedAt = Instant.now();
                                entityManager.persist(new WhatsAppContactNotice(request, job.id));
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
                    String generated;
                    if ("LINK".equals(job.responseKind)) {
                        // Token y respuesta se guardan juntos; un 429 reutiliza el mismo enlace.
                        generated = tx.execute(status -> {
                            String reply = accounts.createReply(job.recipient);
                            em.find(WhatsAppReply.class, job.id).replyText = reply;
                            return reply;
                        });
                    } else if (job.responseKind != null && (job.responseKind.startsWith("CART")
                            || List.of("CHECKOUT", "ORDERS", "BOOKING", "BOOKING_CONFIRM", "BOOKING_DISCARD").contains(job.responseKind)
                            || (job.account() != null && List.of("AI", "RESERVATIONS").contains(job.responseKind)))) {
                        WhatsAppCartService.Reply reply;
                        try {
                            var context = new WhatsAppCartService.Context(job.id, job.recipient, job.account());
                            if (!accounts.matches(job.recipient, job.account())) {
                                reply = WhatsAppCartService.Reply.text(WhatsAppAccountService.LINK_REQUIRED);
                            } else {
                                reply = switch (job.responseKind) {
                                    case "CHECKOUT", "ORDERS", "RESERVATIONS", "BOOKING", "BOOKING_CONFIRM", "BOOKING_DISCARD" ->
                                            cartAssistant.customerCommand(context, job.responseKind, job.incomingText);
                                    case "CART" -> WhatsAppCartService.Reply.text(cart.summary(context));
                                    case "CART_CONFIRM" -> cart.decide(context, job.incomingText, true);
                                    case "CART_CANCEL" -> cart.decide(context, job.incomingText, false);
                                    default -> cartAssistant.answer(context, job.incomingText, tx.execute(status -> history(job)));
                                };
                            }
                        } catch (com.example.demo.errors.ApiException denied) {
                            reply = WhatsAppCartService.Reply.text(denied.getMessage());
                        }
                        var result = reply;
                        generated = result.text();
                        tx.executeWithoutResult(status -> {
                            var stored = em.find(WhatsAppReply.class, job.id);
                            stored.replyText = result.text(); stored.cartActionId = result.actionId();
                        });
                        job.cartActionId = result.actionId();
                    } else {
                        List<WhatsAppAssistant.Turn> history = tx.execute(status -> history(job));
                        String fixed = menu.fixedAnswer(job.responseKind == null ? "AI" : job.responseKind);
                        generated = fixed != null ? fixed : assistant.answer(job.incomingText, history);
                        String answer = generated;
                        tx.executeWithoutResult(status -> em.find(WhatsAppReply.class, job.id).replyText = answer);
                    }
                    job.replyText = generated;
                }
                // También se revisa al reintentar un 429: nunca enviar datos de un vínculo revocado.
                if (job.account() != null && !accounts.matches(job.recipient, job.account())) {
                    job.replyText = WhatsAppAccountService.LINK_REQUIRED;
                    job.cartActionId = null;
                    tx.executeWithoutResult(status -> {
                        var stored = em.find(WhatsAppReply.class, job.id);
                        stored.replyText = WhatsAppAccountService.LINK_REQUIRED; stored.cartActionId = null;
                    });
                }
                // Gemini puede tardar; comprobar de nuevo la ventana antes del envío.
                if (!job.expiresAt.isAfter(Instant.now())) {
                    tx.executeWithoutResult(status -> {
                        var expired = em.find(WhatsAppReply.class, job.id);
                        expired.state = "EXPIRED";
                        if ("LINK".equals(expired.responseKind)) expired.replyText = null;
                    });
                    continue;
                }
                outbound = job.cartActionId != null && job.cartActionId.startsWith("booking:") ? client.sendBookingConfirmation(job.recipient, job.replyText, job.cartActionId.substring(8))
                        : job.cartActionId != null ? client.sendCartConfirmation(job.recipient, job.replyText, job.cartActionId)
                        : "MENU".equals(job.responseKind) ? client.sendMenu(job.recipient, job.replyText)
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
                // El secreto solo permanece en la cola mientras hace falta para reintentar.
                if ("LINK".equals(stored.responseKind) && !"PENDING".equals(resultState)) stored.replyText = null;
                stored.nextAttemptAt = Instant.now().plusSeconds(60L * stored.attempts);
            });
            log.info("Respuesta de WhatsApp: estado={}, error={}", state, error);
        }
    }

    private List<WhatsAppAssistant.Turn> history(WhatsAppReply job) {
        String owner = job.account() == null ? "r.accountClientId is null"
                : "r.accountClientId = :owner and r.accountLinkedAt = :linked";
        var query = em.createQuery("select r from WhatsAppReply r where r.recipient = :recipient and r.state = 'ACCEPTED' and (r.responseKind is null or r.responseKind <> 'LINK') and (" + owner + ") and r.expiresAt > :now and r.expiresAt <= :current and r.id <> :id and r.incomingText is not null and r.replyText is not null order by r.expiresAt desc, r.id desc", WhatsAppReply.class)
                .setParameter("recipient", job.recipient).setParameter("now", Instant.now())
                .setParameter("current", job.expiresAt).setParameter("id", job.id).setMaxResults(6);
        if (job.account() != null) query.setParameter("owner", job.accountClientId).setParameter("linked", job.accountLinkedAt);
        var previous = query.getResultList();
        var turns = new ArrayList<WhatsAppAssistant.Turn>();
        for (var item : previous) {
            String text = "CART_CONFIRM".equals(item.responseKind) ? "Confirmar cambio del carrito"
                    : "CART_CANCEL".equals(item.responseKind) ? "Cancelar cambio del carrito" : item.incomingText;
            turns.add(new WhatsAppAssistant.Turn(text, item.replyText));
        }
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
        if (!job.expiresAt.isAfter(Instant.now())) {
            job.state = "EXPIRED";
            if ("LINK".equals(job.responseKind)) job.replyText = null;
            return null;
        }
        job.state = "SENDING";
        job.attempts++;
        return job;
    }
}
