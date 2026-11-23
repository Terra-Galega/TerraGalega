package com.example.demo.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.OperatorReservationDTO;
import com.example.demo.service.ReservationService;

import io.swagger.v3.oas.annotations.Operation;

// Reservas vistas desde el panel del operador. El rol OPERATOR se exige en SecurityConfig.
@RestController
@RequestMapping("/reservations")
public class OperatorReservationController {

    @Autowired
    private ReservationService reservationService;

    @Operation(summary = "Active reservations (paid, shift not finished), soonest first")
    @GetMapping("/active")
    public List<OperatorReservationDTO> active() {
        return reservationService.getActiveReservations();
    }
}