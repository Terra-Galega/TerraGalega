package com.example.demo.whatsapp;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import jakarta.persistence.*;
import com.example.demo.entities.ReservationTheme;

@Entity
@Table(name = "whatsapp_reservation_proposals")
class ReservationProposal {
    @Id @Column(length = 64) String id;
    String phone;
    Integer clientId;
    Instant linkedAt;
    Instant expiresAt;
    String operation;
    String original;
    Integer tableId;
    LocalDate date;
    LocalTime time;
    Integer seats;
    String name;
    @Enumerated(EnumType.STRING) ReservationTheme theme;
    String result;

    protected ReservationProposal() {}
}
