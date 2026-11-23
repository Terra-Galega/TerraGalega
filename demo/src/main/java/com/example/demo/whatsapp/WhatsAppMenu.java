package com.example.demo.whatsapp;

import java.text.Normalizer;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.example.demo.chat.RestaurantInformation;
import tools.jackson.databind.JsonNode;

@Component
public class WhatsAppMenu {
    static final String MENU_BODY = "¡Hola! Soy Galleguito, el asistente de TerraGalega. Elige una opción o escríbeme tu consulta. Puedes escribir menú cuando quieras volver aquí.";
    private final RestaurantInformation information;
    private final String website;

    public WhatsAppMenu(RestaurantInformation information, @Value("${terra.frontend-url:}") String website) {
        this.information = information;
        this.website = website != null && website.startsWith("https://") ? website.replaceAll("/+$", "") : "";
    }

    record Selection(String kind, String text) { }

    Selection selection(JsonNode message) {
        if ("interactive".equals(message.path("type").asText())) {
            var interactive = message.path("interactive");
            String id = "list_reply".equals(interactive.path("type").asText())
                    ? interactive.path("list_reply").path("id").asText() : "";
            return switch (id) {
                case "tg_catalog" -> new Selection("CATALOG", "Ver menú");
                case "tg_recommend" -> new Selection("AI", "Recomiéndame platos del menú de TerraGalega.");
                case "tg_information" -> new Selection("INFO", "Horarios y ubicación");
                case "tg_reservations" -> new Selection("RESERVATIONS", "Información de reservas");
                case "tg_operator" -> new Selection("OPERATOR", "Solicitar contacto de un operador");
                case "tg_link" -> new Selection("LINK", "Vincular mi cuenta");
                default -> null;
            };
        }
        if (!"text".equals(message.path("type").asText())) return null;
        String text = message.path("text").path("body").asText().trim();
        if (text.isBlank()) return null;
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z0-9 ]", " ").replaceAll("\\s+", " ").trim();
        if (List.of("hola", "buenas", "buenos dias", "buenas tardes", "buenas noches", "menu", "opciones", "inicio",
                "hola quiero conocer el menu de terragalega").contains(normalized)) return new Selection("MENU", text);
        // Solo peticiones explícitas; mencionar un operador en cualquier frase no basta.
        if (List.of("vincular", "vincular mi cuenta", "vincular cuenta", "quiero vincular mi cuenta").contains(normalized)) {
            return new Selection("LINK", text);
        }
        if (normalized.matches("(?:(?:quiero|necesito|deseo) )?(?:hablar|comunicarme|contactar)(?: con)? (?:un |una |el |la )?(?:operador|operario|asesor|persona|humano)(?: por favor)?")
                || List.of("operador", "operario", "asesor", "atencion humana").contains(normalized)) {
            return new Selection("OPERATOR", text);
        }
        return new Selection("AI", text);
    }

    String fixedAnswer(String kind) {
        return switch (kind) {
            case "MENU" -> MENU_BODY;
            case "CATALOG" -> "Consulta nuestro menú y sus precios en " + link("/menu") + ". También puedes pedirme una recomendación aquí.";
            case "INFO" -> "Estamos en " + information.address() + ".\nHorario: " + information.openingHours()
                    + "\nContacto: " + information.phone() + " · " + information.email();
            case "RESERVATIONS" -> "Puedes consultar disponibilidad y reservar en " + link("/reservations") + ". Para una reserva se requiere un anticipo no reembolsable. Por aquí puedo orientarte con las reglas generales.";
            case "OPERATOR" -> "Registramos tu solicitud para que un operador se comunique contigo al número desde el que escribes. Mientras tanto, puedes seguir consultándome sobre TerraGalega.";
            default -> null;
        };
    }

    private String link(String path) { return website.isEmpty() ? "la web de TerraGalega" : website + path; }

    static Map<String, Object> interactive(String body) {
        return Map.of("type", "list", "body", Map.of("text", body),
                "action", Map.of("button", "Ver opciones", "sections", List.of(Map.of("title", "TerraGalega", "rows", List.of(
                        row("tg_catalog", "🍽️ Ver menú"), row("tg_recommend", "⭐ Recomendaciones"),
                        row("tg_information", "🕒 Horarios y ubicación"), row("tg_reservations", "📅 Reservas"),
                        row("tg_operator", "👤 Contactar operador"), row("tg_link", "🔗 Vincular mi cuenta"))))));
    }

    private static Map<String, String> row(String id, String title) { return Map.of("id", id, "title", title); }
}
