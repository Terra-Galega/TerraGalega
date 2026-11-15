package com.example.demo.service;

import com.example.demo.dto.*;
import com.example.demo.entities.*;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.movil.service.OrderMirrorService;
import tools.jackson.databind.json.JsonMapper;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;

import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import jakarta.persistence.PersistenceContext;

import org.springframework.beans.factory.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.function.Supplier;

@Service
public class PaymentService {

    @Autowired
    private StripeClient stripe;

    @Autowired
    private OrderRepository orders;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderMirrorService mirror;

    // Los anticipos de reservas también llegan por el mismo webhook de Stripe
    @Autowired
    private ReservationService reservations;

    @Autowired
    private org.springframework.context.ApplicationEventPublisher events;

    @Autowired
    private InvoiceService invoices;

    @PersistenceContext
    private EntityManager em;

    @Value("${terra.stripe.webhook-secret:}")
    private String webhookSecret;

    // Stripe devuelve al cliente a esta app de Angular al pagar o al cancelar
    @Value("${terra.frontend-url}")
    private String frontendUrl;

    private final TransactionTemplate tx;

    public PaymentService(
            @Qualifier("transactionManager") PlatformTransactionManager manager) {

        tx = new TransactionTemplate(manager);
    }

    private <T> T transaction(Supplier<T> action) {
        return tx.execute(status -> action.get());
    }

    private Order lock(Integer id) {
        return orders.lockById(id)
                .orElseThrow(() -> ApiException.notFound("Pedido inexistente."));
    }

    private void owner(Order order, Integer clientId) {
        if (order.getClient() == null
                || !order.getClient().getId().equals(clientId)) {
            throw ApiException.forbidden("Este pago no te pertenece.");
        }
    }

    private PaymentDTO view(Order order) {
        return new PaymentDTO(
                order.getId(),
                order.getPaymentState(),
                "PENDING".equals(order.getPaymentState())
                        ? order.getStripeCheckoutUrl()
                        : null,
                order.getPaymentAmount(),
                "COP");
    }

    private RequestOptions options(String key) {
        return RequestOptions.builder()
                .setIdempotencyKey(key)
                .build();
    }

    private RuntimeException unavailable() {
        return new ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "No se pudo consultar Stripe. "
                        + "Reintenta: se conservará el mismo pago.");
    }

    private static final JsonMapper JSON = JsonMapper.builder().build();

    private String fingerprint(CheckoutRequestDTO request) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(JSON.writeValueAsString(request)
                                    .getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    // backendUrl: dirección con la que el cliente llegó al back (p. ej.
    // http://192.168.1.20:8080 desde el celular). La usa la vuelta a la app.
    public PaymentDTO start(
            Integer clientId,
            StartPaymentDTO input,
            String backendUrl) {

        if (input == null
                || input.checkout() == null
                || input.requestId() == null) {
            throw ApiException.badRequest("Faltan los datos del pago.");
        }

        try {
            UUID.fromString(input.requestId());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Identificador inválido.");
        }

        String reference = "tg:" + clientId + ":" + input.requestId();
        String hash = fingerprint(input.checkout());

        // Se guarda antes de llamar a Stripe.
        // Si la conexión falla, el reintento recupera este mismo pedido.
        Integer orderId = transaction(() -> {
            Client client = em.find(
                    Client.class,
                    clientId,
                    LockModeType.PESSIMISTIC_WRITE);

            if (client == null) {
                throw ApiException.notFound("Cliente inexistente.");
            }

            Order existing = orders.findByPaymentReference(reference)
                    .orElse(null);

            if (existing != null) {
                if (!hash.equals(existing.getPaymentRequestHash())) {
                    throw ApiException.conflict(
                            "El intento ya corresponde a otro carrito.");
                }

                return existing.getId();
            }

            Order order = orderService.checkoutForPayment(
                    client,
                    input.checkout());

            long amount = BigDecimal.valueOf(order.getTotal())
                    .movePointRight(2)
                    .setScale(0, RoundingMode.HALF_UP)
                    .longValueExact();

            if (amount <= 0) {
                throw ApiException.badRequest(
                        "El importe debe ser positivo.");
            }

            order.setPaymentReference(reference);
            order.setPaymentRequestHash(hash);
            // Adónde vuelve Stripe: la web de Angular o la página puente
            // que reabre la app móvil (se le suma el id del pedido).
            order.setPaymentReturnUrl("app".equals(input.channel())
                    ? backendUrl + "/payments/app-return?order="
                    : frontendUrl + "/checkout/result?order=");
            order.setPaymentAmount(amount);
            order.setPaymentExpiresAt(
                    Instant.now().getEpochSecond() + 3600);

            return orders.save(order).getId();
        });

        return refresh(orderId, clientId);
    }

    public PaymentDTO refresh(Integer orderId, Integer clientId) {
        return transaction(() -> {
            Order order = lock(orderId);

            if (clientId != null) {
                owner(order, clientId);
            }

            if ("REFUND_PENDING".equals(order.getPaymentState())) {
                refund(order);
            } else if ("PENDING".equals(order.getPaymentState())) {
                Session session = session(order);

                if (session != null) {
                    apply(order, session);
                }
            }

            return view(order);
        });
    }

    private Session session(Order order) {
        try {
            if (order.getStripeSessionId() != null) {
                return stripe.v1().checkout().sessions()
                        .retrieve(order.getStripeSessionId());
            }

            // Stripe exige suficiente tiempo futuro al crear una sesión.
            // Un intento antiguo que nunca llegó a abrirse debe renovarse.
            if (Instant.now().getEpochSecond() >= order.getPaymentExpiresAt() - 1805) {

                order.setPaymentState("EXPIRED");
                order.setStatus(OrderStatus.CANCELLED);
                return null;
            }

            var price = SessionCreateParams.LineItem.PriceData.builder()
                    .setCurrency("cop")
                    .setUnitAmount(order.getPaymentAmount())
                    .setProductData(
                            SessionCreateParams.LineItem.PriceData.ProductData
                                    .builder()
                                    .setName(
                                            "Pedido TerraGalega #"
                                                    + order.getId())
                                    .build())
                    .build();

            var params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    // Sin fijar métodos: Stripe muestra los activos en el panel
                    // (en modo prueba, tarjeta). Los pagos asíncronos llegan por
                    // checkout.session.async_payment_succeeded.
                    .setClientReferenceId(order.getPaymentReference())
                    .setSuccessUrl(order.getPaymentReturnUrl() + order.getId())
                    .setCancelUrl(order.getPaymentReturnUrl() + order.getId()
                            + "&canceled=true")
                    .setExpiresAt(order.getPaymentExpiresAt())
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setPriceData(price)
                                    .setQuantity(1L)
                                    .build())
                    .build();

            Session session = stripe.v1().checkout().sessions()
                    .create(
                            params,
                            options(
                                    "checkout:"
                                            + order.getPaymentReference()));

            order.setStripeSessionId(session.getId());
            order.setStripeCheckoutUrl(session.getUrl());

            return session;

        } catch (StripeException e) {
            throw unavailable();
        }
    }

    private void apply(Order order, Session session) {
        boolean differentSession = order.getStripeSessionId() != null
                && !order.getStripeSessionId()
                        .equals(session.getId());

        if (!Boolean.FALSE.equals(session.getLivemode())
                || !Objects.equals(
                        order.getPaymentReference(),
                        session.getClientReferenceId())
                || !Objects.equals(
                        order.getPaymentAmount(),
                        session.getAmountTotal())
                || !"cop".equals(session.getCurrency())
                || !"payment".equals(session.getMode())
                || differentSession) {

            throw ApiException.conflict(
                    "La transacción no coincide con el pedido.");
        }

        order.setStripeSessionId(session.getId());

        // Un evento repetido no vuelve a procesar un pedido pagado
        // ni revierte una cancelación con reembolso.
        if (!Set.of("PENDING", "EXPIRED")
                .contains(order.getPaymentState())) {
            return;
        }

        if ("complete".equals(session.getStatus())
                && "paid".equals(session.getPaymentStatus())) {

            order.setPaymentState("PAID");
            order.setStripePaymentIntent(session.getPaymentIntent());
            order.setStatus(OrderStatus.PENDING);

            mirror.sync(order);
            mirror.sync(order);

            // Factura electrónica al correo del cliente (sale al guardarse el pago)
            events.publishEvent(new InvoiceMailer.OrderPaidEvent(invoices.snapshot(order)));

        } else if ("expired".equals(session.getStatus())) {
            order.setPaymentState("EXPIRED");
            order.setStatus(OrderStatus.CANCELLED);
        }
    }

    public PaymentDTO cancel(Integer orderId, Integer clientId) {
        // Esta transacción reserva la cancelación antes de reembolsar.
        transaction(() -> {
            Order order = lock(orderId);
            owner(order, clientId);

            if (order.getPaymentReference() == null) {
                orderService.cancelOrder(orderId, clientId);
                return null;
            }

            if (Set.of(
                    "EXPIRED",
                    "REFUNDED",
                    "REFUND_PENDING",
                    "REFUND_FAILED")
                    .contains(order.getPaymentState())) {
                return null;
            }

            if (order.getStatus() != OrderStatus.PENDING) {
                throw ApiException.conflict(
                        "El restaurante ya confirmó este pedido.");
            }

            Session session = session(order);

            if (session != null
                    && "open".equals(session.getStatus())) {
                try {
                    session = stripe.v1().checkout().sessions()
                            .expire(session.getId());

                } catch (StripeException e) {
                    // Puede haberse pagado justo al cancelar.
                    try {
                        session = stripe.v1().checkout().sessions()
                                .retrieve(session.getId());
                    } catch (StripeException ex) {
                        throw unavailable();
                    }

                    if ("open".equals(session.getStatus())) {
                        throw unavailable();
                    }
                }
            }

            if (session != null) {
                apply(order, session);
            }

            if ("PAID".equals(order.getPaymentState())) {
                order.setPaymentState("REFUND_PENDING");
                order.setStatus(OrderStatus.CANCELLED);
                mirror.sync(order);
            }

            return null;
        });

        return refresh(orderId, clientId);
    }

    private void refund(Order order) {
        try {
            var refund = order.getStripeRefundId() == null
                    ? stripe.v1().refunds().create(
                            RefundCreateParams.builder()
                                    .setPaymentIntent(
                                            order.getStripePaymentIntent())
                                    .setAmount(order.getPaymentAmount())
                                    .build(),
                            options(
                                    "refund:"
                                            + order.getPaymentReference()))
                    : stripe.v1().refunds()
                            .retrieve(order.getStripeRefundId());

            order.setStripeRefundId(refund.getId());

            if ("succeeded".equals(refund.getStatus())) {
                order.setPaymentState("REFUNDED");
            } else if (Set.of("failed", "canceled")
                    .contains(refund.getStatus())) {
                order.setPaymentState("REFUND_FAILED");
            }

        } catch (StripeException e) {
            throw unavailable();
        }
    }

    public void webhook(String body, String signature) {
        com.stripe.model.Event event;

        try {
            event = Webhook.constructEvent(
                    body,
                    signature,
                    webhookSecret);
        } catch (Exception e) {
            throw ApiException.badRequest(
                    "Firma de Stripe inválida.");
        }

        if (!Boolean.FALSE.equals(event.getLivemode())) {
            throw ApiException.badRequest(
                    "Solo se aceptan eventos de prueba.");
        }

        if (!Set.of(
                "checkout.session.completed",
                "checkout.session.expired",
                "checkout.session.async_payment_succeeded",
                "checkout.session.async_payment_failed")
                .contains(event.getType())) {
            return;
        }

        String sessionId = JSON.readTree(body)
                .path("data")
                .path("object")
                .path("id")
                .asString();

        if (sessionId.isEmpty()) {
            return;
        }

        try {
            Session session = stripe.v1().checkout().sessions()
                    .retrieve(sessionId);

            if (session.getClientReferenceId() == null) {
                return;
            }

            if (session.getClientReferenceId().startsWith(ReservationServiceImpl.REFERENCE_PREFIX)) {
                reservations.handleStripeSession(session);
                return;
            }

            transaction(() -> {
                orders.findByPaymentReference(
                        session.getClientReferenceId())
                        .ifPresent(found -> apply(lock(found.getId()), session));

                return null;
            });

        } catch (StripeException e) {
            throw unavailable();
        }
    }

    // Recupera pagos y reembolsos pendientes aunque el cliente cierre la app.
    @Scheduled(fixedDelay = 60000)
    public void reconcile() {
        List<Integer> ids = transaction(() -> orders.findByPaymentStateIn(
                List.of("PENDING", "REFUND_PENDING"))
                .stream()
                .map(Order::getId)
                .toList());

        for (Integer id : ids) {
            try {
                refresh(id, null);
            } catch (RuntimeException e) {
                org.slf4j.LoggerFactory.getLogger(getClass())
                        .warn(
                                "Pago del pedido {} pendiente de reconciliación",
                                id);
            }
        }
    }
}