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
@Getter
@Setter
@ToString(exclude = "client")
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

    // Mesa con su piso, ej. "Piso 1 - Mesa 112" o, en las salas de celebración,
    // "Piso 2 - Mesa de celebración 231 (Sala 1)"
    @Column(nullable = false, length = 80)
    private String tableLabel;

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
}