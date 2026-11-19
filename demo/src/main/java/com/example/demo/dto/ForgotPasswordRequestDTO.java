package com.example.demo.dto;

// Cuerpo de POST /auth/forgot-password: el correo de la cuenta
public record ForgotPasswordRequestDTO(String email) {
}
