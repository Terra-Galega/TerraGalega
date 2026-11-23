package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;

/**
 * Solo con el perfil prod (terra.production-checks=true). Los valores por defecto de
 * application.properties están en el repositorio: con ellos cualquiera podría firmar
 * un JWT de administrador o un enlace de factura.
 */
@Configuration
@ConditionalOnProperty(name = "terra.production-checks", havingValue = "true")
public class ProductionChecks {
    private static final Logger log = LoggerFactory.getLogger(ProductionChecks.class);
    private static final String REPO_JWT_SECRET = "Py6FLgYBcH4nBg+Xf/bGQyVdA0bdZ7OsjjXSX7TgQMpVoiOx2QQ0/tFCv8r4zNgk11Pge8Z4Fjbg7uNFE2zcKw==";
    private static final String REPO_INVOICE_SECRET = "cambia-esto-por-un-texto-largo-y-secreto";

    @Value("${terra.jwt.secret}") String jwtSecret;
    @Value("${terra.invoice.secret}") String invoiceSecret;
    @Value("${terra.frontend-url:}") String frontendUrl;
    @Value("${terra.email-link-url:}") String emailLinkUrl;
    @Value("${terra.public-backend-url:}") String publicBackendUrl;
    @Value("${terra.stripe.webhook-secret:}") String stripeWebhookSecret;
    @Value("${gemini.api-key:}") String geminiKey;
    @Value("${WHATSAPP_APP_SECRET:}") String whatsappAppSecret;

    @PostConstruct
    void check() {
        List<String> fatal = new ArrayList<>();
        if (REPO_JWT_SECRET.equals(jwtSecret) || jwtSecret.length() < 32) fatal.add("JWT_SECRET (aleatorio, 64+ caracteres)");
        if (REPO_INVOICE_SECRET.equals(invoiceSecret) || invoiceSecret.length() < 32) fatal.add("INVOICE_SECRET (aleatorio, 32+ caracteres)");
        if (!fatal.isEmpty()) {
            throw new IllegalStateException("Configura en Railway antes de desplegar: " + String.join(", ", fatal));
        }
        warnIfNotPublic("FRONTEND_URL", frontendUrl);
        warnIfNotPublic("EMAIL_LINK_URL", emailLinkUrl);
        warnIfNotPublic("PUBLIC_BACKEND_URL", publicBackendUrl);
        if (stripeWebhookSecret.isBlank()) log.warn("Producción: falta STRIPE_WEBHOOK_SECRET; los pagos no se confirmarán.");
        if (geminiKey.isBlank()) log.warn("Producción: falta GEMINI_API_KEY; el chat responderá que no está disponible.");
        if (whatsappAppSecret.isBlank()) log.warn("Producción: falta WHATSAPP_APP_SECRET; el webhook de WhatsApp rechazará los eventos.");
    }

    private static void warnIfNotPublic(String name, String url) {
        if (url == null || !url.startsWith("https://")) {
            log.warn("Producción: {} debería ser la URL pública HTTPS (valor actual no es https).", name);
        }
    }
}
