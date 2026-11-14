package com.example.demo.dto;

// Cuerpo de POST /payments/start.
// requestId: UUID que genera el cliente por cada intento de pago. Si se reenvía
// el mismo (p. ej. tras un fallo de red) se recupera el mismo pedido y pago.
// channel: "app" si lo pide la app móvil (Stripe vuelve a la app);
// vacío o "web" para la web de Angular.
public record StartPaymentDTO(
        String requestId,
        CheckoutRequestDTO checkout,
        String channel) {
}
