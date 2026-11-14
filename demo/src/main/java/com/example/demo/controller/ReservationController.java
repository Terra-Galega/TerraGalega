package com.example.demo.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.ReservationDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;

// Reserva del cliente con sesión iniciada (máximo una, relación 1 a 1). El
// cliente se obtiene del token; /clients/me/** ya exige el rol CLIENT en SecurityConfig.
@RestController
@RequestMapping("/clients/me/reservation")
@CrossOrigin(origins = "http://localhost:4200")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Operation(summary = "Get the reservation of the logged-in client (404 if none)")
    @GetMapping
    public ReservationDTO getReservation() {
        return reservationService.getReservation(authenticatedUser.client().getId());
    }

    @Operation(summary = "Create the reservation of the logged-in client (requires accepting the deposit)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationDTO createReservation(@RequestBody ReservationDTO dto) {
        return reservationService.createReservation(authenticatedUser.client().getId(), dto);
    }

    @Operation(summary = "Update the reservation of the logged-in client")
    @PutMapping
    public ReservationDTO updateReservation(@RequestBody ReservationDTO dto) {
        return reservationService.updateReservation(authenticatedUser.client().getId(), dto);
    }

    @Operation(summary = "Delete the reservation of the logged-in client (the deposit is NOT refunded)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteReservation() {
        reservationService.deleteReservation(authenticatedUser.client().getId());
    }

    @Operation(summary = "Tables already reserved by other clients at a date and time")
    @GetMapping("/taken")
    public List<String> getTakenTables(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(pattern = "HH:mm") LocalTime time) {
        return reservationService.getTakenTables(authenticatedUser.client().getId(), date, time);
    }
}