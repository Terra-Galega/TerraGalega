package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.dto.ReservationDTO;

// Operaciones sobre la reserva del cliente con sesión iniciada.
public interface ReservationService {

    // 404 si el cliente aún no tiene reserva
    ReservationDTO getReservation(Integer clientId);

    ReservationDTO createReservation(Integer clientId, ReservationDTO dto);

    ReservationDTO updateReservation(Integer clientId, ReservationDTO dto);

    // Sin reembolso del anticipo
    void deleteReservation(Integer clientId);

    // Mesas ya reservadas por OTROS clientes en esa fecha y hora
    List<String> getTakenTables(Integer clientId, LocalDate date, LocalTime time);
}