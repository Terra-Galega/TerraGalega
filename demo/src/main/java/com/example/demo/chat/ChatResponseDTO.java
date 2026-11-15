package com.example.demo.chat;

import com.example.demo.dto.ReservationDTO;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Respuesta del chat.
@Getter
@AllArgsConstructor
public class ChatResponseDTO {

    private String reply;

    // true si el bot cambió el carrito: el front debe llamar a cartService.refresh()
    private boolean cartChanged;

    // Reserva creada o consultada en este mensaje (trae checkoutUrl si falta pagar); null si no hubo
    private ReservationDTO reservation;
}