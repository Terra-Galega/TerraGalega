package com.example.demo.dto;

// Cuerpo de POST /auth/google: el ID token que Google entregó al front
public record GoogleLoginRequestDTO(String idToken) {
}
