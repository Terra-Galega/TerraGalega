package com.example.demo.chat;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.dto.ReservationDTO;
import com.example.demo.entities.DiningTable;
import com.example.demo.entities.Product;
import com.example.demo.entities.ReservationTheme;
import com.example.demo.errors.ApiException;
import com.example.demo.movil.dto.CartDTOs.CartAddOnDTO;
import com.example.demo.movil.dto.CartDTOs.CartItemDTO;
import com.example.demo.movil.service.CartService;
import com.example.demo.repository.DiningTableRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ReservationRules;
import com.example.demo.service.ReservationService;
import com.google.genai.types.FunctionDeclaration;
import com.google.genai.types.Schema;
import com.google.genai.types.Tool;
import com.google.genai.types.Type;

// Funciones que Gemini puede pedir ejecutar. Cada una usa los servicios normales del
// back, así que aplican las mismas validaciones que la web.
@Component
public class ChatTools {

    private static final Locale CO = Locale.forLanguageTag("es-CO");

    @Autowired
    private CartService cartService;

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private DiningTableRepository diningTableRepository;

    @Autowired
    private ProductRepository productRepository;

    // Lo que cambió durante un mensaje, para avisarle al front
    public static class Effects {
        boolean cartChanged;
        ReservationDTO reservation;
    }

    // --- Declaraciones que se envían a Gemini ---

    public Tool tool() {
        List<String> slots = ReservationRules.SLOTS.stream().map(LocalTime::toString).toList();
        List<String> themes = Arrays.stream(ReservationTheme.values()).map(Enum::name).toList();

        return Tool.builder().functionDeclarations(
                FunctionDeclaration.builder()
                        .name("agregar_al_carrito")
                        .description("Agrega un plato del menú al carrito del cliente. Úsala solo si el cliente lo pide o acepta una recomendación.")
                        .parameters(object(Map.of(
                                "productId", integer("Id del plato según el menú"),
                                "cantidad", integer("Unidades, de 1 a 99")),
                                "productId", "cantidad"))
                        .build(),
                FunctionDeclaration.builder()
                        .name("ver_carrito")
                        .description("Devuelve los platos que el cliente tiene en el carrito y el total.")
                        .build(),
                FunctionDeclaration.builder()
                        .name("consultar_disponibilidad")
                        .description("Revisa si hay una mesa libre para esa fecha, turno y número de personas. No crea nada.")
                        .parameters(object(Map.of(
                                "fecha", string("Fecha en formato AAAA-MM-DD"),
                                "hora", options("Turno de la reserva", slots),
                                "personas", integer("Número de personas, de 1 a 20")),
                                "fecha", "hora", "personas"))
                        .build(),
                FunctionDeclaration.builder()
                        .name("crear_reserva")
                        .description("Crea la reserva y abre el pago del anticipo. Úsala solo después de que el cliente confirme explícitamente.")
                        .parameters(object(Map.of(
                                "fecha", string("Fecha en formato AAAA-MM-DD"),
                                "hora", options("Turno de la reserva", slots),
                                "personas", integer("Número de personas, de 1 a 20"),
                                "nombre", string("Nombre corto de la reserva, ej. 'Cumpleaños de Ana' (opcional)"),
                                "tematica", options("Motivo de la reserva (NONE si no hay)", themes),
                                "anticipoAceptado", bool("true solo si el cliente aceptó pagar el anticipo no reembolsable")),
                                "fecha", "hora", "personas", "anticipoAceptado"))
                        .build(),
                FunctionDeclaration.builder()
                        .name("ver_mi_reserva")
                        .description("Devuelve la reserva actual del cliente y el estado del pago del anticipo.")
                        .build())
                .build();
    }

    // Menú activo en texto, para el system prompt
    @Transactional(readOnly = true)
    public String menuText() {
        StringBuilder sb = new StringBuilder();
        for (Product p : productRepository.findByActiveTrue()) {
            sb.append("- id ").append(p.getId())
                    .append(" | ").append(p.getName())
                    .append(" | ").append(money(p.getPrice()))
                    .append(" | ").append(p.getCategory() != null ? p.getCategory().getName() : "Sin categoría");
            String tags = tags(p);
            if (!tags.isEmpty()) {
                sb.append(" | ").append(tags);
            }
            if (p.getDescription() != null && !p.getDescription().isBlank()) {
                String d = p.getDescription().trim();
                sb.append(" | ").append(d.length() > 90 ? d.substring(0, 90) + "..." : d);
            }
            sb.append('\n');
        }
        return sb.length() == 0 ? "(el menú está vacío)\n" : sb.toString();
    }

    // --- Ejecución ---

    public Map<String, Object> execute(String name, Map<String, Object> args, Integer clientId, Effects effects) {
        try {
            return switch (name) {
                case "agregar_al_carrito" -> addToCart(clientId, args, effects);
                case "ver_carrito" -> cartSummary(cartService.getCart(clientId));
                case "consultar_disponibilidad" -> availability(clientId, args);
                case "crear_reserva" -> createReservation(clientId, args, effects);
                case "ver_mi_reserva" -> myReservation(clientId, effects);
                default -> error("Función desconocida: " + name);
            };
        } catch (ApiException e) {
            return error(e.getMessage());
        } catch (DateTimeParseException | IllegalArgumentException e) {
            return error("Datos con formato inválido: la fecha va como AAAA-MM-DD y la hora como HH:mm.");
        }
    }

    private Map<String, Object> addToCart(Integer clientId, Map<String, Object> args, Effects effects) {
        Integer productId = intArg(args, "productId");
        Integer quantity = intArg(args, "cantidad");
        if (productId == null) {
            return error("Falta el plato.");
        }
        CheckoutItemDTO item = new CheckoutItemDTO();
        item.setProductId(productId);
        item.setQuantity(quantity == null ? 1 : quantity);
        item.setAddOnIds(List.of());

        List<CartItemDTO> cart = cartService.addItem(clientId, item);
        effects.cartChanged = true;

        Map<String, Object> result = cartSummary(cart);
        result.put("agregado", true);
        return result;
    }

    private Map<String, Object> cartSummary(List<CartItemDTO> cart) {
        List<Map<String, Object>> lines = new ArrayList<>();
        double total = 0;
        for (CartItemDTO line : cart) {
            double unit = line.product().price() == null ? 0 : line.product().price();
            for (CartAddOnDTO a : line.addOns()) {
                unit += a.price() == null ? 0 : a.price();
            }
            double subtotal = unit * line.quantity();
            total += subtotal;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("plato", line.product().name());
            m.put("cantidad", line.quantity());
            m.put("subtotal", money(subtotal));
            lines.add(m);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("lineas", lines);
        result.put("total", money(total));
        return result;
    }

    private Map<String, Object> availability(Integer clientId, Map<String, Object> args) {
        LocalDate date = LocalDate.parse(strArg(args, "fecha"));
        LocalTime time = LocalTime.parse(strArg(args, "hora"));
        int seats = ReservationRules.cleanSeats(intArg(args, "personas"));
        ReservationRules.checkSchedule(date, time);

        Map<String, Object> result = new LinkedHashMap<>();
        DiningTable table = findFreeTable(clientId, date, time, seats);
        result.put("disponible", table != null);
        if (table != null) {
            result.put("mesa", table.getId());
            result.put("piso", table.getFloor());
        } else {
            List<String> others = new ArrayList<>();
            for (LocalTime slot : ReservationRules.SLOTS) {
                if (slot.equals(time) || !LocalDateTime.of(date, slot).isAfter(LocalDateTime.now())) {
                    continue;
                }
                if (findFreeTable(clientId, date, slot, seats) != null) {
                    others.add(slot.toString());
                }
            }
            result.put("otrosTurnosLibresEseDia", others);
        }
        result.put("anticipo", money(ReservationRules.DEPOSIT_AMOUNT));
        return result;
    }

    private Map<String, Object> createReservation(Integer clientId, Map<String, Object> args, Effects effects) {
        if (!boolArg(args, "anticipoAceptado")) {
            return error("Antes de reservar, el cliente debe aceptar el anticipo de "
                    + money(ReservationRules.DEPOSIT_AMOUNT) + " (no reembolsable).");
        }
        LocalDate date = LocalDate.parse(strArg(args, "fecha"));
        LocalTime time = LocalTime.parse(strArg(args, "hora"));
        int seats = ReservationRules.cleanSeats(intArg(args, "personas"));
        ReservationRules.checkSchedule(date, time);

        DiningTable table = findFreeTable(clientId, date, time, seats);
        if (table == null) {
            return error("Ya no hay mesas libres para " + seats + " personas el " + date + " a las " + time + ".");
        }

        String name = strArg(args, "nombre");
        if (name.isBlank()) {
            name = "Reserva TerraGalega";
        }
        if (name.length() > ReservationRules.NAME_MAX) {
            name = name.substring(0, ReservationRules.NAME_MAX);
        }

        ReservationDTO dto = new ReservationDTO();
        dto.setName(name);
        dto.setTableId(table.getId());
        dto.setDate(date);
        dto.setTime(time);
        dto.setSeats(seats);
        dto.setTheme(theme(strArg(args, "tematica")));
        dto.setDepositAccepted(true);

        ReservationDTO created = reservationService.createReservation(clientId, dto);
        effects.reservation = created;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("creada", true);
        result.put("mesa", created.getTableId());
        result.put("piso", created.getFloor());
        result.put("fecha", created.getDate().toString());
        result.put("hora", created.getTime().toString());
        result.put("personas", created.getSeats());
        result.put("anticipo", money(created.getDepositAmount()));
        result.put("siguientePaso", "El cliente debe pagar el anticipo con el botón \"Pagar anticipo\" del chat "
                + "en los próximos 30 minutos; si no, la reserva se cancela sola.");
        return result;
    }

    private Map<String, Object> myReservation(Integer clientId, Effects effects) {
        ReservationDTO dto;
        try {
            dto = reservationService.refreshPayment(clientId);
        } catch (ApiException e) {
            if (e.getStatus() == HttpStatus.NOT_FOUND) {
                return Map.of("tieneReserva", false);
            }
            throw e;
        }
        if ("EXPIRED".equals(dto.getPaymentState())) {
            return Map.of("tieneReserva", false, "nota", "La reserva anterior se canceló porque no se pagó el anticipo a tiempo.");
        }
        effects.reservation = dto;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("tieneReserva", true);
        result.put("nombre", dto.getName());
        result.put("fecha", dto.getDate().toString());
        result.put("hora", dto.getTime().toString());
        result.put("personas", dto.getSeats());
        result.put("mesa", dto.getTableId());
        result.put("piso", dto.getFloor());
        result.put("tematica", String.valueOf(dto.getTheme()));
        result.put("estadoPago", "PAID".equals(dto.getPaymentState()) ? "pagado" : "pendiente de pago");
        return result;
    }

    // La mesa libre más pequeña que alcance para esas personas
    private DiningTable findFreeTable(Integer clientId, LocalDate date, LocalTime time, int seats) {
        List<Integer> taken = reservationService.getTakenTables(clientId, date, time);
        return diningTableRepository.findAll().stream()
                .filter(t -> Boolean.TRUE.equals(t.getAvailable()))
                .filter(t -> t.getSeats() >= seats)
                .filter(t -> !taken.contains(t.getId()))
                .min(Comparator.comparing(DiningTable::getSeats).thenComparing(DiningTable::getId))
                .orElse(null);
    }

    // --- Utilidades ---

    private static String tags(Product p) {
        List<String> t = new ArrayList<>();
        if (Boolean.TRUE.equals(p.getPopular())) t.add("favorito de la casa");
        if (p.isVegetarian()) t.add("vegetariano");
        if (p.isSpicyMild()) t.add("picante suave");
        if (p.isSpicyHot()) t.add("muy picante");
        if (p.isContainsSeafood()) t.add("con mariscos");
        if (p.isContainsNuts()) t.add("con frutos secos");
        if (p.isContainsGluten()) t.add("con gluten");
        return String.join(", ", t);
    }

    private static ReservationTheme theme(String value) {
        try {
            return value.isBlank() ? ReservationTheme.NONE : ReservationTheme.valueOf(value.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return ReservationTheme.NONE;
        }
    }

    private static String money(Double value) {
        return String.format(CO, "$%,.0f", value == null ? 0 : value);
    }

    private static Map<String, Object> error(String message) {
        return Map.of("error", message);
    }

    private static Integer intArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s && !s.isBlank()) return Integer.valueOf(s.trim());
        return null;
    }

    private static String strArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        return v == null ? "" : v.toString().trim();
    }

    private static boolean boolArg(Map<String, Object> args, String key) {
        Object v = args.get(key);
        if (v instanceof Boolean b) return b;
        return v != null && "true".equalsIgnoreCase(v.toString().trim());
    }

    private static Schema object(Map<String, Schema> properties, String... required) {
        return Schema.builder().type(Type.Known.OBJECT).properties(properties).required(List.of(required)).build();
    }

    private static Schema integer(String description) {
        return Schema.builder().type(Type.Known.INTEGER).description(description).build();
    }

    private static Schema string(String description) {
        return Schema.builder().type(Type.Known.STRING).description(description).build();
    }

    private static Schema bool(String description) {
        return Schema.builder().type(Type.Known.BOOLEAN).description(description).build();
    }

    private static Schema options(String description, List<String> values) {
        return Schema.builder().type(Type.Known.STRING).description(description).enum_(values).build();
    }
}