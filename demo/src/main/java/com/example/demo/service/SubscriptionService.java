package com.example.demo.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.dto.SubscriptionDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.SubscriptionPayment;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.SubscriptionPaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

// Suscripción Premium (domicilios gratis) pagada con Stripe Checkout:
// 1. start valida la regla de renovación y abre una sesión de Stripe (o reutiliza la abierta).
// 2. El cliente paga y Stripe lo devuelve a /account?subscription=done del front.
// 3. status consulta a Stripe: si se pagó, el pago pasa a PAID y se extiende la fecha de
//    vencimiento. Eso ocurre una sola vez porque el pago se bloquea mientras se procesa.
// "Premium" no se guarda: se calcula comparando Client.subscriptionEndsAt con la hora actual.
@Service
public class SubscriptionService {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionService.class);

    // Prefijo de las referencias de suscripciones en Stripe
    public static final String REFERENCE_PREFIX = "tgs:";

    // Precio fijo en pesos colombianos
    public static final long PRICE_COP = 65000L;

    public static final int DURATION_DAYS = 30;

    // Con este número de días o menos restantes se permite renovar
    public static final int RENEWAL_WINDOW_DAYS = 5;

    // Stripe exige que la sesión dure al menos 30 min
    private static final long PAYMENT_WINDOW_SECONDS = 31 * 60;

    @Autowired
    private StripeClient stripe;

    @Autowired
    private SubscriptionPaymentRepository payments;

    @PersistenceContext
    private EntityManager em;

    // Stripe devuelve al cliente a esta app de Angular al pagar o al cancelar
    @Value("${terra.frontend-url}")
    private String frontendUrl;

    private final TransactionTemplate tx;

    public SubscriptionService(
            @Qualifier("transactionManager") PlatformTransactionManager manager) {
        tx = new TransactionTemplate(manager);
    }

    private <T> T transaction(Supplier<T> action) {
        return tx.execute(status -> action.get());
    }

    // --- Operaciones públicas ---

    // Estado actual. Primero asienta los pagos pendientes consultando a Stripe, así el
    // front puede consultar este endpoint después de pagar aunque el webhook no llegue.
    public SubscriptionDTO status(Integer clientId) {
        return transaction(() -> {
            settlePending(clientId);
            Client client = em.find(Client.class, clientId);
            if (client == null) {
                throw ApiException.notFound("Cliente inexistente.");
            }
            return view(client, null);
        });
    }

    // Abre el pago. La regla vive aquí: no se puede pagar si ya es premium con más de 5
    // días restantes, aunque el front lo permita.
    public SubscriptionDTO start(Integer clientId) {
        return transaction(() -> {
            Client client = lockClient(clientId);
            if (client == null) {
                throw ApiException.notFound("Cliente inexistente.");
            }

            // Si Stripe ya resolvió un pago anterior, se asienta antes de decidir
            settlePending(clientId);

            if (!canSubscribe(client)) {
                throw ApiException.conflict(
                        "Tu suscripción Premium sigue vigente. Podrás renovarla cuando te queden "
                                + RENEWAL_WINDOW_DAYS + " días o menos.");
            }

            // Si ya hay un pago abierto y vigente, se reutiliza su misma sesión de Stripe
            long now = Instant.now().getEpochSecond();
            SubscriptionPayment open = payments
                    .findByClientIdAndPaymentState(clientId, SubscriptionPayment.PENDING)
                    .stream()
                    .filter(p -> p.getPaymentExpiresAt() > now && p.getStripeCheckoutUrl() != null)
                    .findFirst()
                    .orElse(null);
            if (open != null) {
                return view(client, open);
            }

            // Se guarda antes de llamar a Stripe. Si Stripe falla, la transacción se
            // deshace y no queda nada a medias.
            SubscriptionPayment payment = payments.save(SubscriptionPayment.builder()
                    .client(client)
                    .paymentReference(REFERENCE_PREFIX + clientId + ":" + UUID.randomUUID())
                    .paymentState(SubscriptionPayment.PENDING)
                    .paymentAmount(amountInCents())
                    .paymentExpiresAt(now + PAYMENT_WINDOW_SECONDS)
                    .build());

            openStripeSession(payment);
            return view(client, payment);
        });
    }
    
    // Webhook de Stripe (lo llama PaymentService para las referencias "tgs:").
    // settle vuelve a consultar la sesión a Stripe y bloquea el pago, así que da igual
    // si el webhook llega repetido o al mismo tiempo que la consulta del front.
    public void handleStripeSession(Session session) {
        transaction(() -> {
            payments.findByPaymentReference(session.getClientReferenceId())
                    .ifPresent(payment -> {
                        log.info("Webhook de Stripe para la suscripción {}", payment.getPaymentReference());
                        settle(payment.getId());
                    });
            return null;
        });
    }

    // Recupera pagos pendientes aunque el cliente cierre la pestaña y el webhook no
    // llegue: pagados -> PAID (y se extiende la fecha); caducados -> EXPIRED.
    // Cada pago va en su propia transacción: si uno falla, los demás siguen.
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    public void reconcile() {
        List<Integer> ids = transaction(() -> payments
                .findByPaymentState(SubscriptionPayment.PENDING)
                .stream()
                .map(SubscriptionPayment::getId)
                .toList());

        if (!ids.isEmpty()) {
            log.info("Tarea programada: revisando {} pago(s) de suscripción pendiente(s)", ids.size());
        }

        for (Integer id : ids) {
            try {
                transaction(() -> {
                    settle(id);
                    return null;
                });
            } catch (RuntimeException e) {
                log.warn("Pago de suscripción {} pendiente de reconciliación: {}", id, e.toString());
            }
        }
    }

    // Al borrar la cuenta: cierra las sesiones de Stripe abiertas (para que no se pueda
    // pagar después) y borra los intentos de pago del cliente.
    public void deleteClientPayments(Integer clientId) {
        transaction(() -> {
            for (SubscriptionPayment payment : payments
                    .findByClientIdAndPaymentState(clientId, SubscriptionPayment.PENDING)) {
                Session session = retrieveSession(payment);
                if (session != null && "open".equals(session.getStatus())) {
                    try {
                        stripe.v1().checkout().sessions().expire(session.getId());
                    } catch (StripeException e) {
                        throw unavailable();
                    }
                }
            }
            payments.deleteByClientId(clientId);
            return null;
        });
    }

    // --- Lógica de pagos ---

    private static long amountInCents() {
        return PRICE_COP * 100;
    }

    // Se puede pagar si es freemium, o si es premium y está en la ventana de renovación
    private static boolean canSubscribe(Client client) {
        return !client.isSubscribed() || client.getDaysLeft() <= RENEWAL_WINDOW_DAYS;
    }

    private void settlePending(Integer clientId) {
        for (SubscriptionPayment payment : payments
                .findByClientIdAndPaymentState(clientId, SubscriptionPayment.PENDING)) {
            settle(payment.getId());
        }
    }

    // Consulta a Stripe y aplica el resultado: pagada -> PAID (y se extiende la fecha);
    // caducada -> EXPIRED. Es idempotente: un pago ya resuelto no se vuelve a procesar.
    private void settle(Integer paymentId) {
        // Se bloquea la fila: si otra petición (webhook, tarea programada) está
        // procesando este mismo pago, esta espera a que termine.
        SubscriptionPayment payment = em.find(
                SubscriptionPayment.class, paymentId, LockModeType.PESSIMISTIC_WRITE);
        if (payment == null) {
            return;
        }

        // Estado real en la base de datos: la entidad pudo cargarse antes del bloqueo
        // y otra petición pudo marcarla como PAID mientras tanto.
        List<String> states = em.createQuery(
                "select p.paymentState from SubscriptionPayment p where p.id = :id", String.class)
                .setParameter("id", paymentId)
                .getResultList();
        if (states.isEmpty() || !SubscriptionPayment.PENDING.equals(states.get(0))) {
            if (!states.isEmpty()) {
                payment.setPaymentState(states.get(0));
            }
            return;
        }

        Session session = retrieveSession(payment);
        if (session != null) {
            checkSessionMatches(payment, session);
            if (isPaid(session)) {
                markPaid(payment, session);
                return;
            }
        }

        boolean expired = session != null
                ? "expired".equals(session.getStatus())
                : Instant.now().getEpochSecond() >= payment.getPaymentExpiresAt();
        if (expired) {
            payment.setPaymentState(SubscriptionPayment.EXPIRED);
            payment.setStripeCheckoutUrl(null);
        }
    }
    // Marca el pago y extiende la suscripción:
    // - si sigue activa: fecha actual de vencimiento + 30 días (no se pierden días)
    // - si ya venció o nunca existió: ahora + 30 días
    private void markPaid(SubscriptionPayment payment, Session session) {
        payment.setPaymentState(SubscriptionPayment.PAID);
        payment.setStripePaymentIntent(session.getPaymentIntent());
        payment.setStripeCheckoutUrl(null);

        Client client = lockClient(payment.getClient().getId());
        if (client == null) {
            log.warn("Pago de suscripción {} recibido de un cliente que ya no existe", payment.getId());
            return;
        }

        LocalDateTime base = client.isSubscribed() ? client.getSubscriptionEndsAt() : LocalDateTime.now();
        client.setSubscriptionEndsAt(base.plusDays(DURATION_DAYS));
    }

    // Bloquea al cliente y trae de la base su fecha real de vencimiento.
    // Primero se carga normal y DESPUÉS se bloquea con em.lock: así el bloqueo es un
    // "select id ... for update" simple. Cargar y bloquear en una sola consulta arrastra
    // los joins de sus relaciones (reserva, usuario, roles) y H2 lo rechaza; pasaba en la
    // tarea programada y en el webhook, donde el cliente no venía cargado por el token.
    private Client lockClient(Integer clientId) {
        Client client = em.find(Client.class, clientId);
        if (client == null) {
            return null;
        }
        em.lock(client, LockModeType.PESSIMISTIC_WRITE);

        List<LocalDateTime> dates = em.createQuery(
                "select c.subscriptionEndsAt from Client c where c.id = :id", LocalDateTime.class)
                .setParameter("id", clientId)
                .getResultList();
        client.setSubscriptionEndsAt(dates.isEmpty() ? null : dates.get(0));
        return client;
    }
    private SubscriptionDTO view(Client client, SubscriptionPayment payment) {
        SubscriptionPayment last = payment != null
                ? payment
                : payments.findFirstByClientIdOrderByIdDesc(client.getId()).orElse(null);
        boolean pending = last != null && SubscriptionPayment.PENDING.equals(last.getPaymentState());

        return new SubscriptionDTO(
                client.isSubscribed(),
                client.getSubscriptionEndsAt(),
                client.getDaysLeft(),
                canSubscribe(client),
                PRICE_COP,
                DURATION_DAYS,
                last != null ? last.getPaymentState() : null,
                pending ? last.getStripeCheckoutUrl() : null);
    }

    // --- Stripe ---

    private void openStripeSession(SubscriptionPayment payment) {
        String returnUrl = frontendUrl + "/account?subscription=done";

        var price = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("cop")
                .setUnitAmount(payment.getPaymentAmount())
                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Suscripción Premium TerraGalega - " + DURATION_DAYS + " días")
                        .setDescription("Domicilios gratis durante " + DURATION_DAYS
                                + " días. No reembolsable.")
                        .build())
                .build();

        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(payment.getPaymentReference())
                .setSuccessUrl(returnUrl)
                .setCancelUrl(returnUrl + "&canceled=true")
                .setExpiresAt(payment.getPaymentExpiresAt())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(price)
                        .setQuantity(1L)
                        .build())
                .build();

        try {
            Session session = stripe.v1().checkout().sessions().create(params,
                    RequestOptions.builder()
                            .setIdempotencyKey("subscription:" + payment.getPaymentReference())
                            .build());
            payment.setStripeSessionId(session.getId());
            payment.setStripeCheckoutUrl(session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe rechazó la sesión de suscripción {}: {}",
                    payment.getPaymentReference(), e.getMessage());
            throw unavailable();
        }
    }

    private Session retrieveSession(SubscriptionPayment payment) {
        if (payment.getStripeSessionId() == null) {
            return null;
        }
        try {
            return stripe.v1().checkout().sessions().retrieve(payment.getStripeSessionId());
        } catch (StripeException e) {
            log.error("Stripe no devolvió la sesión {}: {}",
                    payment.getStripeSessionId(), e.getMessage());
            throw unavailable();
        }
    }

    // La sesión de Stripe debe ser de prueba, de este pago, por este monto y en COP
    private void checkSessionMatches(SubscriptionPayment payment, Session session) {
        if (!Boolean.FALSE.equals(session.getLivemode())
                || !Objects.equals(payment.getPaymentReference(), session.getClientReferenceId())
                || !Objects.equals(payment.getPaymentAmount(), session.getAmountTotal())
                || !"cop".equals(session.getCurrency())
                || !"payment".equals(session.getMode())) {
            throw ApiException.conflict("La transacción no coincide con la suscripción.");
        }
    }

    private static boolean isPaid(Session session) {
        return "complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus());
    }

    private RuntimeException unavailable() {
        return ApiException.serviceUnavailable(
                "No se pudo conectar con la pasarela de pago. Inténtalo de nuevo en unos minutos.");
    }
}