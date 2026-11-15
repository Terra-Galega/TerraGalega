package com.example.demo.config;

import com.stripe.StripeClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@EnableAsync

public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    @Bean
    public StripeClient stripeClient(
            @Value("${terra.stripe.secret-key:}") String key) {

        String apiKey = key.trim();

        // Sin clave la app arranca igual (útil para quien no trabaja en pagos):
        // Stripe rechazará las llamadas y PaymentService responderá 503.
        if (apiKey.isEmpty()) {
            log.warn("STRIPE_SECRET_KEY no está configurada: los pagos en línea no funcionarán.");
            apiKey = "sk_test_no_configurada";
        } else if (!apiKey.startsWith("sk_test_")) {
            throw new IllegalStateException(
                    "Usa la clave sk_test_ del sandbox de Stripe.");
        }

        return StripeClient.builder()
                .setApiKey(apiKey)
                .setConnectTimeout(10000)
                .setReadTimeout(30000)
                .setMaxNetworkRetries(2)
                .build();
    }
}
