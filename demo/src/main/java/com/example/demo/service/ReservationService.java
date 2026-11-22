package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.dto.OperatorReservationDTO;
import com.example.demo.dto.ReservationDTO;
import com.stripe.model.checkout.Session;

// Operaciones sobre la reserva del cliente con sesión iniciada.
public interface ReservationService {

    // 404 si el cliente aún no tiene reserva
    ReservationDTO getReservation(Integer clientId);

    // Crea la reserva en PENDING y la sesión de Stripe del anticipo (checkoutUrl)
    ReservationDTO createReservation(Integer clientId, ReservationDTO dto);

    ReservationDTO updateReservation(Integer clientId, ReservationDTO dto);

    // Sin reembolso del anticipo (si estaba pendiente, cierra la sesión de Stripe)
    void deleteReservation(Integer clientId);

    // Estado del pago del anticipo consultando a Stripe: PENDING, PAID o EXPIRED
    ReservationDTO refreshPayment(Integer clientId);

    // Sesión de Stripe recibida por el webhook con referencia de reserva ("tgr:")
    void handleStripeSession(Session session);

    // Revisa los pagos pendientes (tarea programada)
    void reconcile();

    // Números de las mesas ya reservadas por OTROS clientes en esa fecha y hora
    List<Integer> getTakenTables(Integer clientId, LocalDate date, LocalTime time);
    
    // Reservas pagadas cuyo turno aún no termina, de la más próxima a la más lejana (operador)
    List<OperatorReservationDTO> getActiveReservations();
}