package com.example.demo.entities;

import java.time.LocalDate;
import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

// Reserva de mesa de un cliente. La relación con Client es 1 a 1 en ambos
// sentidos: un cliente tiene como máximo una reserva y cada reserva es de un
// solo cliente. La FK (unique) vive aquí, en client_id.
// Con DiningTable es N a 1: cada reserva tiene una mesa y una mesa puede tener
// de 0 a muchas reservas en distintas fechas y horas.
@Getter
@Setter
@ToString(exclude = { "client", "table" })
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
@Table(name = "reservations")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(unique = true, nullable = false)
    private Integer id;

    // Cliente dueño de la reserva. unique = true es lo que hace la relación 1 a 1
    // en la base de datos. Se ignora en el JSON para evitar la recursión
    // Client -> Reservation -> Client.
    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    private Client client;

    // Nombre con el que el cliente identifica la reserva (ej. "Cena de grado")
    @Column(nullable = false, length = 60)
    private String name;

    // Mesa reservada (FK dining_table_id). Se ignora en el JSON; el DTO envía
    // su id y su piso.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dining_table_id", nullable = false)
    private DiningTable table;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false)
    private LocalTime time;

    // Número de sillas de la reserva
    @Column(nullable = false)
    private Integer seats;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationTheme theme;

    // --- Pago del anticipo con Stripe Checkout ---
    // PENDING: creada, esperando el pago (la mesa queda apartada mientras tanto).
    // PAID: anticipo pagado, reserva confirmada.
    // Si la sesión de Stripe caduca sin pagar, la reserva se elimina y la mesa se libera.
    @Column(nullable = false, length = 20)
    private String paymentState;

    // Referencia propia enviada a Stripe como client_reference_id ("tgr:...")
    @Column(unique = true, length = 80)
    private String paymentReference;

    private String stripeSessionId;

    @Column(length = 1000)
    private String stripeCheckoutUrl;

    private String stripePaymentIntent;

    // Segundos epoch en que caduca la sesión de Stripe
    private Long paymentExpiresAt;

    public boolean isPaid() {
        return PAID.equals(paymentState);
    }

    public static final String PENDING = "PENDING";
    public static final String PAID = "PAID";
}