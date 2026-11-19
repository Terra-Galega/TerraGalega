package com.example.demo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.example.demo.errors.ApiException;

// Reglas de las reservas.
public final class ReservationRules {

    // Anticipo que se paga al reservar.
    // No se reembolsa si la reserva se elimina.
    public static final double DEPOSIT_AMOUNT = 50000.0;

    // Turnos válidos (los mismos de la página de reservas, cada hora y media)
    public static final List<LocalTime> SLOTS = List.of(
            LocalTime.of(12, 0), LocalTime.of(13, 30), LocalTime.of(15, 0),
            LocalTime.of(16, 30), LocalTime.of(18, 0), LocalTime.of(19, 30),
            LocalTime.of(21, 0));

    // Lo que dura un turno (los turnos van cada hora y media). También es el tiempo
    // que una mesa ocupada por un mesero queda bloqueada para reservas de hoy.
    public static final int SLOT_MINUTES = 90;

    public static final int NAME_MAX = 60;
    // Mesas de celebración: hasta 20 sillas
    public static final int MIN_SEATS = 1;
    public static final int MAX_SEATS = 20;

    private ReservationRules() {
    }

    // Nombre obligatorio, sin espacios sobrantes y con máximo de caracteres
    public static String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw ApiException.badRequest("Escribe el nombre de la reserva.");
        }
        String clean = name.trim();
        if (clean.length() > NAME_MAX) {
            throw ApiException.badRequest(
                    "El nombre de la reserva no puede superar " + NAME_MAX + " caracteres.");
        }
        return clean;
    }

    public static Integer cleanSeats(Integer seats) {
        if (seats == null || seats < MIN_SEATS || seats > MAX_SEATS) {
            throw ApiException.badRequest(
                    "El número de sillas debe estar entre " + MIN_SEATS + " y " + MAX_SEATS + ".");
        }
        return seats;
    }

    // Fecha y hora obligatorias, hora dentro de los turnos y en el futuro
    public static void checkSchedule(LocalDate date, LocalTime time) {
        if (date == null || time == null) {
            throw ApiException.badRequest("Elige la fecha y la hora de la reserva.");
        }
        if (!SLOTS.contains(time)) {
            throw ApiException.badRequest("Esa hora no es un turno de reserva válido.");
        }
        if (!LocalDateTime.of(date, time).isAfter(LocalDateTime.now())) {
            throw ApiException.badRequest("La reserva debe ser para una fecha y hora futuras.");
        }
    }
}