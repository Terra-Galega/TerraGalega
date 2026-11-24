package com.example.demo.whatsapp;

import java.time.Duration;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;

@Component
public class WhatsAppMessageClient {
    static final String TEST_REPLY = "¡Hola! Recibimos tu mensaje en TerraGalega. Esta es una respuesta automática de prueba; el asistente todavía está en preparación.";
    private final RestClient http;
    private final WhatsAppReplySettings settings;

    @Autowired
    public WhatsAppMessageClient(WhatsAppReplySettings settings) {
        this.settings = settings;
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(10));
        this.http = RestClient.builder().baseUrl("https://graph.facebook.com")
                .requestFactory(factory).build();
    }

    WhatsAppMessageClient(WhatsAppReplySettings settings, RestClient http) {
        this.settings = settings;
        this.http = http;
    }

    public String send(String recipient, String text) {
        if (text == null || text.isBlank() || text.length() > 4096) {
            throw new IllegalArgumentException("Texto de WhatsApp invalido");
        }
        return sendPayload(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", recipient,
                "type", "text", "text", Map.of("body", text, "preview_url", false)));
    }

    public String sendMenu(String recipient, String text) {
        return sendPayload(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", recipient,
                "type", "interactive", "interactive", WhatsAppMenu.interactive(text)));
    }

    public String sendCartConfirmation(String recipient, String text, String actionId) {
        if (text == null || text.isBlank() || text.length() > 1024 || actionId == null || !actionId.matches("[a-f0-9]{64}")) {
            throw new IllegalArgumentException("Confirmación de carrito inválida");
        }
        return sendDecision(recipient, text, "tg_cart_confirm:" + actionId, "tg_cart_cancel:" + actionId);
    }

    public String sendBookingConfirmation(String recipient, String text, String proposal) {
        return sendDecision(recipient, text, "book_yes:" + proposal, "book_no:" + proposal);
    }

    private String sendDecision(String recipient, String text, String confirmId, String cancelId) {
        return sendPayload(Map.of("messaging_product", "whatsapp", "recipient_type", "individual", "to", recipient,
                "type", "interactive", "interactive", Map.of("type", "button", "body", Map.of("text", text),
                        "action", Map.of("buttons", java.util.List.of(
                                Map.of("type", "reply", "reply", Map.of("id", confirmId, "title", "✅ Confirmar")),
                                Map.of("type", "reply", "reply", Map.of("id", cancelId, "title", "Cancelar")))))));
    }

    private String sendPayload(Map<String, Object> payload) {
        JsonNode response = http.post().uri("/{version}/{phone}/messages", settings.apiVersion(), settings.phoneNumberId())
                .headers(headers -> headers.setBearerAuth(settings.accessToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .body(payload)
                .retrieve().body(JsonNode.class);
        String id = response == null ? "" : response.path("messages").path(0).path("id").asText();
        if (id.isBlank() || id.length() > 255) throw new IllegalStateException("Respuesta de Meta sin identificador valido");
        return id;
    }
}
