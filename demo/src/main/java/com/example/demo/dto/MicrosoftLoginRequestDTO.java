package com.example.demo.dto;

// El código se envía únicamente al confirmar el primer acceso con esta identidad.
public record MicrosoftLoginRequestDTO(String idToken, String confirmationCode) {}