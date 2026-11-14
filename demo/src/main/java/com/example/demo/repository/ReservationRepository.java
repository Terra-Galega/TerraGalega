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
    Optional<Reservation> findByTableIdAndDateAndTime(Integer tableId, LocalDate date, LocalTime time);
}