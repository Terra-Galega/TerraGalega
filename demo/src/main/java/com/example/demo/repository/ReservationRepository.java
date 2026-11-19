package com.example.demo.repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.Reservation;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Integer> {

    // La reserva de un cliente (1 a 1)
    Optional<Reservation> findByClientId(Integer clientId);

    boolean existsByClientId(Integer clientId);

    // Reservas de una fecha y hora: sirven para saber qué mesas están ocupadas
    List<Reservation> findByDateAndTime(LocalDate date, LocalTime time);

    // ¿Esa mesa ya está reservada en esa fecha y hora?
    Optional<Reservation> findByPaymentReference(String paymentReference);

    List<Reservation> findByPaymentState(String paymentState);

    Optional<Reservation> findByTableIdAndDateAndTime(Integer tableId, LocalDate date, LocalTime time);

    // Pedidos de mesa: reservas de un piso en una franja de hoy (el aviso
    // "reserva en 2 horas o menos" del portal del mesero)
    List<Reservation> findByTableFloorAndDateAndTimeBetweenOrderByTimeAsc(Integer floor, LocalDate date,
            LocalTime from, LocalTime to);

    // Reservas de un piso cuyo turno ya empezó y sigue en curso (time entre from y to)
    List<Reservation> findByTableFloorAndDateAndTimeBetweenOrderByTimeDesc(Integer floor, LocalDate date,
            LocalTime from, LocalTime to);
}