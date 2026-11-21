package com.example.demo.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.DiningTable;
import com.example.demo.entities.Reservation;
import com.example.demo.entities.ReservationTheme;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DiningTableRepository;
import com.example.demo.repository.ReservationRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.checkout.SessionCreateParams;

// Reservas con anticipo obligatorio por Stripe Checkout:
// 1. createReservation valida mesa, sillas, fecha y disponibilidad, guarda la reserva en
//    PENDING (la mesa queda apartada) y abre una sesión de Stripe por el anticipo.
// 2. El cliente paga en Stripe y vuelve a /reservations/result del front.
// 3. refreshPayment (o el webhook, o la tarea programada) consulta Stripe: si se pagó, la
//    reserva pasa a PAID; si la sesión caducó, la reserva se elimina y la mesa se libera.
@Service
public class ReservationServiceImpl implements ReservationService {

    private static final Logger log = LoggerFactory.getLogger(ReservationServiceImpl.class);

    // Prefijo de las referencias de reservas en Stripe (los pedidos usan "tg:")
    public static final String REFERENCE_PREFIX = "tgr:";

    // Tiempo que la mesa queda apartada esperando el pago. Stripe exige al menos 30 min.
    private static final long PAYMENT_WINDOW_SECONDS = 31 * 60;

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private DiningTableRepository diningTableRepository;

    // Mesas ocupadas ahora por un mesero (pedidos de mesa): bloquean los turnos de
    // hoy que empiezan antes de que pasen SLOT_MINUTES desde que se ocupó la mesa
    @Autowired
    private com.example.demo.repository.TableOrderRepository tableOrderRepository;

    @Autowired
    private StripeClient stripe;

    // Stripe devuelve al cliente a esta app de Angular al pagar o al cancelar
    @Value("${terra.frontend-url}")
    private String frontendUrl;

    @Override
    @Transactional
    public ReservationDTO getReservation(Integer clientId) {
        return toDTO(findOwned(clientId));
    }

    // Crea la reserva PENDING y la sesión de pago del anticipo. Si Stripe falla, la
    // transacción se deshace y no queda ninguna reserva a medias.
    @Override
    @Transactional
    public ReservationDTO createReservation(Integer clientId, ReservationDTO dto) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> ApiException.notFound("No se encontró el cliente solicitado."));

        if (reservationRepository.existsByClientId(clientId)) {
            throw ApiException.conflict(
                    "Ya tienes una reserva. Edítala o elimínala para hacer otra.");
        }
        if (!Boolean.TRUE.equals(dto.getDepositAccepted())) {
            throw ApiException.badRequest(
                    "Debes aceptar el pago del anticipo para crear la reserva.");
        }

        String name = ReservationRules.cleanName(dto.getName());
        Integer seats = ReservationRules.cleanSeats(dto.getSeats());
        DiningTable table = findTable(dto.getTableId(), seats);
        ReservationRules.checkSchedule(dto.getDate(), dto.getTime());
        checkTableFree(table, dto.getDate(), dto.getTime(), null);

        Reservation reservation = Reservation.builder()
                .client(client)
                .name(name)
                .table(table)
                .date(dto.getDate())
                .time(dto.getTime())
                .seats(seats)
                .theme(dto.getTheme() != null ? dto.getTheme() : ReservationTheme.NONE)
                .paymentState(Reservation.PENDING)
                .paymentReference(REFERENCE_PREFIX + clientId + ":" + UUID.randomUUID())
                .paymentExpiresAt(Instant.now().getEpochSecond() + PAYMENT_WINDOW_SECONDS)
                .build();
        // Se enlaza también por el lado del cliente para mantener el 1 a 1 coherente
        client.setReservation(reservation);
        reservation = saveChecked(reservation);

        openStripeSession(reservation);
        return toDTO(reservation);
    }

    // Cambia los datos de la reserva (el anticipo ya está pagado, no se vuelve a pedir)
    @Override
    @Transactional
    public ReservationDTO updateReservation(Integer clientId, ReservationDTO dto) {
        Reservation reservation = findOwned(clientId);
        if (!reservation.isPaid()) {
            throw ApiException.conflict("Termina de pagar el anticipo antes de editar la reserva.");
        }

        String name = ReservationRules.cleanName(dto.getName());
        Integer seats = ReservationRules.cleanSeats(dto.getSeats());
        DiningTable table = findTable(dto.getTableId(), seats);
        ReservationRules.checkSchedule(dto.getDate(), dto.getTime());
        checkTableFree(table, dto.getDate(), dto.getTime(), reservation.getId());

        // La entidad está gestionada: se guarda sola al terminar la transacción
        reservation.setName(name);
        reservation.setTable(table);
        reservation.setDate(dto.getDate());
        reservation.setTime(dto.getTime());
        reservation.setSeats(seats);
        reservation.setTheme(dto.getTheme() != null ? dto.getTheme() : ReservationTheme.NONE);
        // Se guarda ya (y no al terminar) para responder 409 si la base rechaza un choque
        return toDTO(saveChecked(reservation));
    }

    // Elimina la reserva. Si estaba pagada, el anticipo NO se reembolsa. Si el pago
    // seguía pendiente, se cierra la sesión de Stripe para que ya no se pueda pagar.
    @Override
    @Transactional
    public void deleteReservation(Integer clientId) {
        Reservation reservation = findOwned(clientId);

        if (!reservation.isPaid()) {
            Session session = retrieveSession(reservation);
            if (session != null && "open".equals(session.getStatus())) {
                try {
                    session = stripe.v1().checkout().sessions().expire(session.getId());
                } catch (StripeException e) {
                    throw unavailable();
                }
            }
            if (session != null && isPaid(session)) {
                markPaid(reservation, session);
                throw ApiException.conflict(
                        "El anticipo ya se había pagado: tu reserva quedó confirmada.");
            }
        }
        remove(reservation);
    }

    // Estado del pago del anticipo; consulta a Stripe por si el webhook aún no llegó
    @Override
    @Transactional
    public ReservationDTO refreshPayment(Integer clientId) {
        Reservation reservation = findOwned(clientId);
        if (reservation.isPaid()) {
            return toDTO(reservation);
        }
        return syncWithStripe(reservation, retrieveSession(reservation));
    }

    // Webhook de Stripe (lo llama PaymentService para las referencias "tgr:")
    @Override
    @Transactional
    public void handleStripeSession(Session session) {
        reservationRepository.findByPaymentReference(session.getClientReferenceId())
                .filter(r -> !r.isPaid())
                .ifPresent(r -> syncWithStripe(r, session));
    }

    // Recupera pagos pendientes aunque el cliente cierre la pestaña y libera las mesas
    // de las reservas que caducaron sin pagar.
    @Override
    @Scheduled(fixedDelay = 60000, initialDelay = 60000)
    @Transactional
    public void reconcile() {
        for (Reservation reservation : reservationRepository.findByPaymentState(Reservation.PENDING)) {
            try {
                syncWithStripe(reservation, retrieveSession(reservation));
            } catch (RuntimeException e) {
                log.warn("Pago de la reserva {} pendiente de reconciliación", reservation.getId());
            }
        }
    }

    @Override
    @Transactional
    public List<Integer> getTakenTables(Integer clientId, LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw ApiException.badRequest("Elige la fecha y la hora.");
        }
        // Cuentan las pagadas y las que esperan pago. La mesa del propio cliente no
        // cuenta, así puede conservarla al editar.
        java.util.Set<Integer> taken = new java.util.LinkedHashSet<>();
        reservationRepository.findByDateAndTime(date, time).stream()
                .filter(r -> !r.getClient().getId().equals(clientId))
                .forEach(r -> taken.add(r.getTable().getId()));
        // Una mesa que un mesero acaba de ocupar no se puede reservar para el turno siguiente
        taken.addAll(blockedByOccupation(date, time).keySet());
        return List.copyOf(taken);
    }

    // --- Stripe ---

    private void openStripeSession(Reservation reservation) {
        DiningTable table = reservation.getTable();
        String returnUrl = frontendUrl + "/reservations/result?reservation=" + reservation.getId();

        var price = SessionCreateParams.LineItem.PriceData.builder()
                .setCurrency("cop")
                .setUnitAmount(depositInCents())
                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                        .setName("Anticipo de reserva TerraGalega - Mesa " + table.getId())
                        .setDescription("Piso " + table.getFloor() + ", " + reservation.getDate()
                                + " a las " + reservation.getTime() + ". No reembolsable.")
                        .build())
                .build();

        var params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setClientReferenceId(reservation.getPaymentReference())
                .setSuccessUrl(returnUrl)
                .setCancelUrl(returnUrl + "&canceled=true")
                .setExpiresAt(reservation.getPaymentExpiresAt())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setPriceData(price)
                        .setQuantity(1L)
                        .build())
                .build();

        try {
            Session session = stripe.v1().checkout().sessions().create(params,
                    RequestOptions.builder()
                            .setIdempotencyKey("reservation:" + reservation.getPaymentReference())
                            .build());
            reservation.setStripeSessionId(session.getId());
            reservation.setStripeCheckoutUrl(session.getUrl());
        } catch (StripeException e) {
            throw unavailable();
        }
    }

    // El anticipo en centavos, como lo pide Stripe para COP (igual que los pedidos)
    private static long depositInCents() {
        return Math.round(ReservationRules.DEPOSIT_AMOUNT * 100);
    }

    private Session retrieveSession(Reservation reservation) {
        if (reservation.getStripeSessionId() == null) {
            return null;
        }
        try {
            return stripe.v1().checkout().sessions().retrieve(reservation.getStripeSessionId());
        } catch (StripeException e) {
            throw unavailable();
        }
    }

    // Aplica el estado de la sesión de Stripe: pagada -> PAID; caducada -> se elimina.
    private ReservationDTO syncWithStripe(Reservation reservation, Session session) {
        if (session != null) {
            checkSessionMatches(reservation, session);
            if (isPaid(session)) {
                markPaid(reservation, session);
                return toDTO(reservation);
            }
        }
        boolean expired = session != null
                ? "expired".equals(session.getStatus())
                : Instant.now().getEpochSecond() >= reservation.getPaymentExpiresAt();
        if (expired) {
            ReservationDTO dto = toDTO(reservation);
            dto.setPaymentState("EXPIRED");
            dto.setCheckoutUrl(null);
            remove(reservation);
            return dto;
        }
        return toDTO(reservation);
    }

    private void checkSessionMatches(Reservation reservation, Session session) {
        if (!Boolean.FALSE.equals(session.getLivemode())
                || !Objects.equals(reservation.getPaymentReference(), session.getClientReferenceId())
                || !Objects.equals(depositInCents(), session.getAmountTotal())
                || !"cop".equals(session.getCurrency())) {
            throw ApiException.conflict("La transacción no coincide con la reserva.");
        }
    }

    private static boolean isPaid(Session session) {
        return "complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus());
    }

    private void markPaid(Reservation reservation, Session session) {
        reservation.setPaymentState(Reservation.PAID);
        reservation.setStripePaymentIntent(session.getPaymentIntent());
        reservation.setStripeCheckoutUrl(null);
    }

    private RuntimeException unavailable() {
        return ApiException.serviceUnavailable(
                "No se pudo conectar con la pasarela de pago. Inténtalo de nuevo en unos minutos.");
    }

    // --- Utilidades ---

    // Última red de seguridad: si aun así la base de datos rechaza la reserva
    // (misma mesa y turno, o el cliente ya tiene otra), se responde 409 con un
    // mensaje claro en vez de un error genérico
    private Reservation saveChecked(Reservation reservation) {
        try {
            return reservationRepository.saveAndFlush(reservation);
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            throw ApiException.conflict("Esa mesa se acaba de reservar en ese turno, o ya tienes otra reserva. "
                    + "Recarga y elige otra mesa u otra hora.");
        }
    }

    private void remove(Reservation reservation) {
        // orphanRemoval en Client.reservation la borra de la BD
        reservation.getClient().setReservation(null);
        reservation.getTable().getReservations().remove(reservation);
        reservationRepository.delete(reservation);
    }

    private Reservation findOwned(Integer clientId) {
        return reservationRepository.findByClientId(clientId)
                .orElseThrow(() -> ApiException.notFound("Aún no tienes una reserva."));
    }

    // La mesa debe existir en el plano y tener sillas suficientes. available NO se
    // mira: indica si está ocupada ahora en el salón, y una mesa ocupada hoy se
    // puede reservar para otro turno. Los choques entre reservas los revisa checkTableFree.
    private DiningTable findTable(Integer tableId, Integer seats) {
        if (tableId == null) {
            throw ApiException.badRequest("Elige una mesa.");
        }
        // Bloqueada hasta el final de la transacción: si dos clientes reservan la misma
        // mesa a la vez, el segundo espera y ya ve la reserva del primero
        DiningTable table = diningTableRepository.lockById(tableId)
                .orElseThrow(() -> ApiException.badRequest("La mesa no es válida."));
        if (seats > table.getSeats()) {
            throw ApiException.badRequest(
                    "Esa mesa admite como máximo " + table.getSeats() + " sillas.");
        }
        return table;
    }

    // Verifica la disponibilidad. ignoreId: al editar, la propia reserva no cuenta.
    private void checkTableFree(DiningTable table, LocalDate date, LocalTime time, Integer ignoreId) {
        boolean taken = reservationRepository.findByTableIdAndDateAndTime(table.getId(), date, time).stream()
                .anyMatch(r -> !r.getId().equals(ignoreId));
        if (taken) {
            throw ApiException.conflict("Esa mesa ya está reservada en esa fecha y hora.");
        }

        java.time.LocalDateTime until = blockedByOccupation(date, time).get(table.getId());
        if (until != null) {
            throw ApiException.conflict("La mesa " + table.getId() + " está ocupada en el restaurante hasta las "
                    + until.toLocalTime().withSecond(0).withNano(0) + ". Elige un turno más tarde u otra mesa.");
        }
    }

    // Mesas ocupadas en el salón que bloquean ese turno: si un mesero ocupó la
    // mesa a la hora S, queda bloqueada desde S hasta S + SLOT_MINUTES, redondeado
    // hacia arriba a la siguiente media hora. Devuelve id de la mesa -> hora de desbloqueo.
    // Ejemplo: ocupada a las 14:25 -> bloqueada hasta las 16:00 (15:00 no, 16:30 sí).
    private java.util.Map<Integer, java.time.LocalDateTime> blockedByOccupation(LocalDate date, LocalTime time) {
        java.util.Map<Integer, java.time.LocalDateTime> blocked = new java.util.HashMap<>();
        if (date == null || time == null) {
            return blocked;
        }
        java.time.LocalDateTime slot = java.time.LocalDateTime.of(date, time);
        for (Object[] row : tableOrderRepository.findOccupationStarts(java.util.Set.of(
                com.example.demo.entities.TableOrderStatus.PENDING,
                com.example.demo.entities.TableOrderStatus.IN_PROGRESS))) {
            java.time.LocalDateTime since = (java.time.LocalDateTime) row[1];
            java.time.LocalDateTime until = roundUpToHalfHour(since.plusMinutes(ReservationRules.SLOT_MINUTES));
            if (!slot.isBefore(since) && slot.isBefore(until)) {
                blocked.put((Integer) row[0], until);
            }
        }
        return blocked;
    }

    // 15:55 -> 16:00, 15:20 -> 15:30, 15:30 -> 15:30 (ya es una media hora exacta)
    private static java.time.LocalDateTime roundUpToHalfHour(java.time.LocalDateTime time) {
        java.time.LocalDateTime base = time.withSecond(0).withNano(0);
        boolean exact = base.equals(time) && base.getMinute() % 30 == 0;
        if (exact) {
            return base;
        }
        int minutesToAdd = 30 - (base.getMinute() % 30);
        return base.plusMinutes(minutesToAdd);
    }

    private ReservationDTO toDTO(Reservation r) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(r.getId());
        dto.setClientId(r.getClient().getId());
        dto.setName(r.getName());
        dto.setTableId(r.getTable().getId());
        dto.setFloor(r.getTable().getFloor());
        dto.setDate(r.getDate());
        dto.setTime(r.getTime());
        dto.setSeats(r.getSeats());
        dto.setTheme(r.getTheme());
        dto.setDepositAmount(ReservationRules.DEPOSIT_AMOUNT);
        dto.setPaymentState(r.getPaymentState());
        dto.setCheckoutUrl(r.isPaid() ? null : r.getStripeCheckoutUrl());
        return dto;
    }
}
