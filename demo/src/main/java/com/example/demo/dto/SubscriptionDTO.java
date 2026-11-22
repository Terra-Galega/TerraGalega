package com.example.demo.dto;

import java.time.LocalDateTime;

// Estado de la suscripción del cliente con sesión. El precio viaja desde aquí para que
// el front no tenga una segunda constante.
public record SubscriptionDTO(
        boolean subscribed,
        LocalDateTime subscriptionEndsAt,
        long daysLeft,
        // true si puede pagar ahora: es freemium, o es premium y le quedan 5 días o
        // menos
        boolean canSubscribe,
        long priceCop,
        int durationDays,
        // Estado del último intento de pago: PENDING, PAID, EXPIRED, o null si nunca ha
        // pagado
        String paymentState,
        // Solo viene con valor mientras el último pago está PENDING
        String checkoutUrl,
        long pointsBalance,
        int pointValueCop) {
}