package com.example.demo.whatsapp;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record WhatsAppReplySettings(boolean enabled, String accessToken, String phoneNumberId, String apiVersion) {
    public WhatsAppReplySettings(
            @Value("${WHATSAPP_AUTO_REPLY_ENABLED:false}") boolean enabled,
            @Value("${WHATSAPP_ACCESS_TOKEN:}") String accessToken,
            @Value("${WHATSAPP_PHONE_NUMBER_ID:}") String phoneNumberId,
            @Value("${WHATSAPP_API_VERSION:v25.0}") String apiVersion) {
        this.enabled = enabled;
        this.accessToken = accessToken.trim();
        this.phoneNumberId = phoneNumberId.trim();
        this.apiVersion = apiVersion.trim();
        if (enabled && (this.accessToken.isEmpty() || !this.phoneNumberId.matches("[0-9]+")
                || !this.apiVersion.matches("v[0-9]+\\.[0-9]+"))) {
            throw new IllegalArgumentException("Configura WHATSAPP_ACCESS_TOKEN, WHATSAPP_PHONE_NUMBER_ID y WHATSAPP_API_VERSION antes de activar respuestas");
        }
    }

    // Un record no debe exponer el token mediante su toString generado.
    @Override public String toString() { return "WhatsAppReplySettings[enabled=" + enabled + "]"; }
}
