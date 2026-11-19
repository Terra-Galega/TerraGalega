package com.example.demo.dto;

// Cuerpo de POST /auth/reset-password: el token del enlace y la contraseña nueva
public record ResetPasswordRequestDTO(String token, String newPassword) {
}
