package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.demo.entities.ReservationTheme;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Reserva del cliente. Al crear o editar se usan name, tableId, date, time,
// seats y theme (el id y el cliente los asigna el back). Al crear, además,
// depositAccepted debe ser true. depositAmount solo sale en las respuestas.
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReservationDTO {

    private Integer id;

    // Id del cliente dueño (solo respuesta; el back lo toma del token)
    private Integer clientId;

    private String name;

    // Número de la mesa del plano (101, 231...)
    private Integer tableId;

    // Piso de la mesa (solo respuesta)
    private Integer floor;

    private LocalDate date;

    private LocalTime time;

    private Integer seats;

    // Vacío = NONE
    private ReservationTheme theme;

    // Solo al crear: el cliente acepta pagar el anticipo (no reembolsable)
    private Boolean depositAccepted;

    // Anticipo de la reserva (solo respuesta)
    private Double depositAmount;

    // Estado del pago del anticipo (solo respuesta): PENDING, PAID o EXPIRED
    // (EXPIRED solo aparece al consultar un pago que caducó: la reserva ya no existe)
    private String paymentState;

    // Página de Stripe donde se paga el anticipo (solo mientras está PENDING)
    private String checkoutUrl;
}