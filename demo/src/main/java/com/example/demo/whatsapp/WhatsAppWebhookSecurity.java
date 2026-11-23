package com.example.demo.whatsapp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class WhatsAppWebhookSecurity {
    private final String verifyToken;
    private final String appSecret;

    public WhatsAppWebhookSecurity(
            @Value("${WHATSAPP_VERIFY_TOKEN:}") String verifyToken,
            @Value("${WHATSAPP_APP_SECRET:}") String appSecret) {
        this.verifyToken = verifyToken;
        this.appSecret = appSecret;
    }

    public boolean configured() {
        return StringUtils.hasText(verifyToken) && StringUtils.hasText(appSecret);
    }

    public boolean verifyToken(String candidate) {
        return configured() && candidate != null && MessageDigest.isEqual(
                verifyToken.getBytes(StandardCharsets.UTF_8), candidate.getBytes(StandardCharsets.UTF_8));
    }

    // Se firma el cuerpo original, antes de convertirlo a JSON.
    public boolean verifySignature(byte[] body, String signature) {
        if (!configured() || signature == null || !signature.matches("sha256=[0-9a-fA-F]{64}")) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(appSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return MessageDigest.isEqual(mac.doFinal(body), HexFormat.of().parseHex(signature.substring(7)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo verificar el webhook", e);
        }
    }
}
