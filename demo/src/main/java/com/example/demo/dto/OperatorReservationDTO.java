package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalTime;

import com.example.demo.entities.ReservationTheme;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Reserva activa tal como la ve el operador: incluye los datos del cliente.
// status: UPCOMING (aún no es la hora), WAITING (ya es la hora, esperando que llegue),
// SEATED (ya llegó y está en la mesa) o NO_SHOW (pasó el margen de espera sin llegar).
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OperatorReservationDTO {
    private Integer id;
    private String name;
    private String clientName;
    private String clientPhone;
    private Integer tableId;
    private Integer floor;
    private LocalDate date;
    private LocalTime time;
    private Integer seats;
    private ReservationTheme theme;
    private String status;
}