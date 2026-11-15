package com.example.demo.entities;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Un intento de pago de la suscripción Premium. Sirve para: reutilizar la sesión de
// Stripe si el cliente vuelve a pulsar el botón, y para que el pago se aplique UNA sola
// vez aunque lo detecten a la vez el webhook, la consulta del front y la tarea programada.
@Entity
@Table(name = "subscription_payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionPayment {

    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
    public static final String EXPIRED = "EXPIRED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    // Dueño del pago. La llave foránea tiene ON DELETE CASCADE: si el cliente se borra
    // por cualquier camino, la base borra también sus intentos de pago y no queda
    // ningún error de integridad. (Cerrar la sesión de Stripe abierta lo hace
    // SubscriptionService.deleteClientPayments al borrar la cuenta.)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Client client;

    // "tgs:<clientId>:<uuid>" (los pedidos usan "tg:" y las reservas "tgr:")
    @Column(nullable = false, unique = true)
    private String paymentReference;

    // PENDING, PAID o EXPIRED
    @Column(nullable = false)
    private String paymentState;

    // En centavos, como lo pide Stripe para COP
    @Column(nullable = false)
    private Long paymentAmount;

    // Segundos desde 1970 en que caduca la sesión de Stripe
    @Column(nullable = false)
    private Long paymentExpiresAt;

    private String stripeSessionId;

    @Column(length = 2000)
    private String stripeCheckoutUrl;

    private String stripePaymentIntent;
}