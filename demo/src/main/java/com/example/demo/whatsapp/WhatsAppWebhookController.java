package com.example.demo.whatsapp;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/webhooks/whatsapp")
public class WhatsAppWebhookController {
    private final WhatsAppWebhookSecurity security;
    private final WhatsAppWebhookInbox inbox;
    private final ObjectMapper mapper;

    public WhatsAppWebhookController(WhatsAppWebhookSecurity security, WhatsAppWebhookInbox inbox, ObjectMapper mapper) {
        this.security = security;
        this.inbox = inbox;
        this.mapper = mapper;
    }

    @GetMapping(produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> verify(
            @RequestParam(name = "hub.mode", required = false) String mode,
            @RequestParam(name = "hub.verify_token", required = false) String token,
            @RequestParam(name = "hub.challenge", required = false) String challenge) {
        if (!security.configured()) {
            return ResponseEntity.status(503).body("Webhook no configurado");
        }
        if (!"subscribe".equals(mode) || challenge == null || challenge.isBlank() || !security.verifyToken(token)) {
            return ResponseEntity.status(403).body("Verificacion rechazada");
        }
        return ResponseEntity.ok(challenge);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> receive(@RequestBody byte[] body,
            @RequestHeader(name = "X-Hub-Signature-256", required = false) String signature) {
        if (!security.configured()) {
            return ResponseEntity.status(503).body("Webhook no configurado");
        }
        if (!security.verifySignature(body, signature)) {
            return ResponseEntity.status(403).body("Firma rechazada");
        }
        try {
            JsonNode event = mapper.readTree(body);
            if (event == null || !"whatsapp_business_account".equals(event.path("object").asText())
                    || !event.path("entry").isArray()) {
                return ResponseEntity.badRequest().body("Evento invalido");
            }
        } catch (JacksonException e) {
            return ResponseEntity.badRequest().body("JSON invalido");
        }
        // El 200 solo se devuelve después de confirmar el guardado en la BD.
        inbox.receive(body);
        return ResponseEntity.ok("EVENT_RECEIVED");
    }
}
