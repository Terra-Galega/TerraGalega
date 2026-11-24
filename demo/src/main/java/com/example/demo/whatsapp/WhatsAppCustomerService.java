package com.example.demo.whatsapp;

import java.time.*;
import java.util.*;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.persistence.*;
import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.*;
import com.example.demo.errors.ApiException;
import com.example.demo.movil.service.CartService;
import com.example.demo.repository.*;
import com.example.demo.service.*;
import com.example.demo.whatsapp.WhatsAppCartService.Context;

@Service
public class WhatsAppCustomerService {
    private final WhatsAppAccountService accounts;
    private final CartService carts;
    private final OrderRepository orders;
    private final DiningTableRepository tables;
    private final ReservationRepository bookings;
    private final ReservationService reservations;
    private final String website;
    @PersistenceContext(unitName = "h2") private EntityManager em;

    public WhatsAppCustomerService(WhatsAppAccountService accounts, CartService carts, OrderRepository orders,
            DiningTableRepository tables, ReservationRepository bookings, ReservationService reservations,
            @Value("${terra.frontend-url:}") String website) {
        this.accounts = accounts;
        this.carts = carts;
        this.orders = orders;
        this.tables = tables;
        this.bookings = bookings;
        this.reservations = reservations;
        this.website = website.replaceAll("/+$", "");
    }

    static final String RESERVATION_HELP = "Para consultar mesas, dime fecha, hora y número de personas. También puedes pedirme crear, cambiar o cancelar tu reserva.";
    WhatsAppCartService.Reply run(Context context, String operation, Map<String,Object> args) {
        return accounts.withAccount(context.phone(), context.account(), () -> {
            int owner = context.account().clientId();
            return switch (operation) {
                case "finalizar_pedido" -> WhatsAppCartService.Reply.text(checkout(owner));
                case "mis_pedidos" -> WhatsAppCartService.Reply.text(orderStatus(owner, args));
                case "mi_reserva" -> WhatsAppCartService.Reply.text(reservationStatus(owner));
                case "disponibilidad", "crear", "cambiar", "cancelar" -> prepare(context, operation, args);
                default -> throw ApiException.badRequest("Elige consultar disponibilidad, crear, cambiar o cancelar la reserva.");
            };
        });
    }

    private String link(String path) {
        if (!website.startsWith("https://")) throw ApiException.serviceUnavailable("El enlace a la web aún no está configurado.");
        return website + path;
    }

    private String checkout(int owner) {
        var lines = carts.getCart(owner);
        String summary = WhatsAppCartService.formatSummary(lines);
        if (lines.isEmpty()) return summary;
        return summary + "\nRevisa la dirección, los descuentos y el importe final e inicia el pago con tu cuenta en " + link("/checkout");
    }

    private String orderStatus(int owner, Map<String,Object> args) {
        List<Order> found;
        if (args.containsKey("pedido")) {
            int id = number(args.get("pedido"));
            found = orders.findByIdAndClientId(id, owner).filter(Order::isVisibleOrder).stream().toList();
        } else {
            found = orders.findByClientIdOrderByCreatedAtDesc(owner).stream().filter(Order::isVisibleOrder).limit(5).toList();
        }
        if (found.isEmpty()) return "No encontré pedidos con esos datos en tu cuenta.";
        var text = new StringBuilder("📦 Tus pedidos");
        for (Order order : found) {
            String state = switch (order.getStatus()) {
                case PENDING -> "pendiente de confirmación";
                case CONFIRMED -> "confirmado";
                case PREPARING -> "en preparación";
                case ON_THE_WAY -> "en camino";
                case DELIVERED -> "entregado";
                case CANCELLED -> "cancelado";
            };
            if ("PENDING".equals(order.getPaymentState())) state = "pendiente de pago";
            if ("EXPIRED".equals(order.getPaymentState())) state = "pago vencido";
            text.append("\n#").append(order.getId()).append(" · ").append(state);
            if (order.getStatus() == OrderStatus.ON_THE_WAY) text.append("\nSeguir domicilio: ").append(link("/orders/tracking?order=" + order.getId()));
        }
        return text + "\nHistorial completo: " + link("/orders");
    }

    private ReservationDTO current(int owner) {
        return bookings.existsByClientId(owner) ? reservations.getReservation(owner) : null;
    }

    private String reservationStatus(int owner) {
        var reservation = current(owner);
        if (reservation == null) return "Todavía no tienes una reserva. " + RESERVATION_HELP;
        String payment = "PAID".equals(reservation.getPaymentState()) ? "Anticipo pagado." : "Anticipo pendiente. Continúa en " + link("/reservations");
        return "📅 Tu reserva: " + details(reservation) + ". " + payment + "\n" + RESERVATION_HELP;
    }

    private WhatsAppCartService.Reply prepare(Context context, String operation, Map<String,Object> args) {
        int owner = context.account().clientId();
        var previous = current(owner);
        if (operation.equals("crear") && previous != null) return WhatsAppCartService.Reply.text("Ya tienes una reserva. " + reservationStatus(owner));
        if ((operation.equals("cancelar") || operation.equals("cambiar")) && previous == null) return WhatsAppCartService.Reply.text("No tienes una reserva que modificar.");
        if (operation.equals("cambiar") && !"PAID".equals(previous.getPaymentState())) return WhatsAppCartService.Reply.text("Para editar tu reserva, completa el anticipo en " + link("/reservations"));
        var proposal = new ReservationProposal();
        String description;
        if (operation.equals("cancelar")) {
            description = "Cancelar tu reserva del " + details(previous) + ". Si pagaste el anticipo, no se reembolsa.";
        } else {
            try {
                proposal.date = LocalDate.parse(String.valueOf(args.get("fecha")));
                proposal.time = LocalTime.parse(String.valueOf(args.get("hora")));
                proposal.seats = ReservationRules.cleanSeats(number(args.get("personas")));
            } catch (DateTimeException invalid) {
                throw ApiException.badRequest("Dime la fecha, el turno y el número de personas para buscar una mesa.");
            }
            ReservationRules.checkSchedule(proposal.date, proposal.time);
            var taken = reservations.getTakenTables(owner, proposal.date, proposal.time);
            var free = tables.findAll().stream().filter(t -> t.getSeats() >= proposal.seats && !taken.contains(t.getId()))
                    .min(Comparator.comparing(DiningTable::getSeats).thenComparing(DiningTable::getId));
            String slot = proposal.date + " a las " + proposal.time + " para " + proposal.seats + " personas";
            if (free.isEmpty()) return WhatsAppCartService.Reply.text("No quedan mesas el " + slot + ". Prueba otro turno.");
            proposal.tableId = free.get().getId();
            if (operation.equals("disponibilidad")) return WhatsAppCartService.Reply.text("Hay disponibilidad el " + slot + ". Si quieres reservar, pídeme crear la reserva.");
            proposal.theme = previous == null ? ReservationTheme.NONE : previous.getTheme();
            proposal.name = previous == null ? "Reserva TerraGalega" : previous.getName();
            description = (operation.equals("crear") ? "Crear una reserva el " : "Cambiar tu reserva al ") + slot + ", mesa " + proposal.tableId + ".";
            if (operation.equals("crear")) description += " Al confirmar aceptas pagar el anticipo no reembolsable de " + String.format(Locale.forLanguageTag("es-CO"), "$%,.0f COP", ReservationRules.DEPOSIT_AMOUNT) + " en la web.";
        }
        proposal.id = WhatsAppCartService.digest(context.messageId()).substring(0, 16);
        proposal.phone = context.phone();
        proposal.clientId = owner;
        proposal.linkedAt = context.account().linkedAt();
        proposal.original = fingerprint(previous);
        proposal.operation = operation;
        proposal.expiresAt = Instant.now().plusSeconds(600);
        if (em.find(ReservationProposal.class, proposal.id) == null) {
            em.createQuery("update ReservationProposal p set p.expiresAt = :now where p.phone = :phone and p.result is null")
                    .setParameter("now", Instant.now()).setParameter("phone", context.phone()).executeUpdate();
            em.persist(proposal);
        }
        return new WhatsAppCartService.Reply(description + "\nRevisa los datos y pulsa Confirmar para aplicar esta propuesta. Vence en 10 minutos.", "booking:" + proposal.id);
    }

    String decide(Context context, String id, boolean accepted) {
        return accounts.withAccount(context.phone(), context.account(), () -> {
            var proposal = em.find(ReservationProposal.class, id, LockModeType.PESSIMISTIC_WRITE);
            if (proposal == null || !context.phone().equals(proposal.phone) || !context.account().clientId().equals(proposal.clientId)
                    || !context.account().linkedAt().equals(proposal.linkedAt)) return "Esta propuesta no está disponible para tu cuenta.";
            if (proposal.result != null) return proposal.result;
            if (!proposal.expiresAt.isAfter(Instant.now())) return "La propuesta venció. Pide otra para continuar.";
            if (!accepted) {
                proposal.result = "Propuesta descartada. Tu reserva sigue igual.";
                return proposal.result;
            }
            var previous = current(proposal.clientId);
            if (!proposal.original.equals(fingerprint(previous))) return "Tu reserva cambió desde la propuesta. Consulta «mi reserva» y pide el cambio de nuevo.";
            if (proposal.operation.equals("cancelar")) {
                reservations.deleteReservation(proposal.clientId);
                proposal.result = "Tu reserva quedó cancelada.";
            } else {
                ReservationDTO dto = new ReservationDTO();
                dto.setName(proposal.name);
                dto.setTableId(proposal.tableId);
                dto.setDate(proposal.date);
                dto.setTime(proposal.time);
                dto.setSeats(proposal.seats);
                dto.setTheme(proposal.theme);
                if (proposal.operation.equals("crear")) {
                    String paymentPage = link("/reservations");
                    dto.setDepositAccepted(true);
                    var created = reservations.createReservation(proposal.clientId, dto);
                    proposal.result = "Reserva creada: " + details(created) + ". Completa el anticipo para confirmarla en " + paymentPage;
                } else {
                    var updated = reservations.updateReservation(proposal.clientId, dto);
                    proposal.result = "Reserva actualizada: " + details(updated) + ".";
                }
            }
            return proposal.result;
        });
    }

    private static String details(ReservationDTO r) {
        return r.getDate() + " a las " + r.getTime() + ", mesa " + r.getTableId() + " para " + r.getSeats() + " personas";
    }
    private static String fingerprint(ReservationDTO r) {
        return r == null ? "none" : WhatsAppCartService.digest(r.getId() + "|" + details(r) + "|" + r.getName() + "|" + r.getTheme() + "|" + r.getPaymentState());
    }
    private static int number(Object value) {
        try { return new BigDecimal(String.valueOf(value)).intValueExact(); }
        catch (IllegalArgumentException | ArithmeticException invalid) { throw ApiException.badRequest("Indica un número entero válido."); }
    }
}
