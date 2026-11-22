package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.PaymentService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/payments")
@CrossOrigin(origins = "http://localhost:4200")
public class PaymentController {

    private final PaymentService payments;
    private final AuthenticatedUser auth;

    public PaymentController(
            PaymentService payments,
            AuthenticatedUser auth) {

        this.payments = payments;
        this.auth = auth;
    }

    @PostMapping("/start")
    public PaymentDTO start(@RequestBody StartPaymentDTO request) {
        // La misma dirección con la que llegó el cliente: desde el celular es
        // la IP del PC, así el navegador del celular puede volver aquí.
        String backendUrl = ServletUriComponentsBuilder
                .fromCurrentContextPath()
                .toUriString();

        return payments.start(auth.client().getId(), request, backendUrl);
    }

    @GetMapping("/{id}")
    public PaymentDTO status(@PathVariable Integer id) {
        return payments.refresh(id, auth.client().getId());
    }

    @PostMapping("/{id}/cancel-pending")
    public PaymentDTO cancelPending(@PathVariable Integer id) {
        return payments.cancelPending(id, auth.client().getId());
    }

    @PostMapping("/{id}/cancel")
    public PaymentDTO cancel(@PathVariable Integer id) {
        return payments.cancel(id, auth.client().getId());
    }

    @PostMapping("/webhook")
    public void webhook(
            @RequestBody String body,
            @RequestHeader("Stripe-Signature") String signature) {

        payments.webhook(body, signature);
    }

    // Stripe solo vuelve a direcciones http(s). Esta página puente reabre la
    // app móvil con su enlace terragalega://pago. No muestra ningún dato: la
    // app consulta el estado real con GET /payments/{id} y su token.
    @GetMapping(value = "/app-return", produces = "text/html;charset=UTF-8")
    public String appReturn(
            @RequestParam Integer order,
            @RequestParam(defaultValue = "false") boolean canceled) {

        String link = "terragalega://pago?order=" + order
                + (canceled ? "&canceled=true" : "");

        return """
                <!doctype html>
                <html lang="es">
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width,initial-scale=1">
                <title>TerraGalega</title>
                <body style="font-family:system-ui;background:#f5f2eb;color:#2c2c2c;
                             padding:40px 24px;text-align:center">
                  <h1 style="font-size:22px">Volviendo a TerraGalega…</h1>
                  <p>Si la app no se abre sola, pulsa el botón.</p>
                  <p><a href="%1$s" style="display:inline-block;margin-top:12px;
                        padding:14px 28px;border-radius:999px;background:#b2571f;
                        color:#fff;text-decoration:none;font-weight:600">Abrir la app</a></p>
                  <script>location.replace("%1$s");</script>
                </body>
                </html>
                """.formatted(link);
    }
}
