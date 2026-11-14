package com.example.demo.service;

import org.springframework.security.crypto.password.PasswordEncoder;

import com.example.demo.errors.ApiException;

// Contiene las reglas de validación para el cambio de contraseña.
public final class PasswordRules {

    private PasswordRules() {
    }

    // storedHash es la contraseña cifrada guardada en users.
    public static void validateChange(String storedHash, String current, String newPassword,
            PasswordEncoder encoder) {

        if (current == null || current.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw ApiException.badRequest("Debes completar todos los campos.");
        }

        // BCrypt no se puede "descifrar": se compara con matches
        if (!encoder.matches(current, storedHash)) {
            throw ApiException.badRequest("La contraseña actual no es correcta.");
        }

        if (newPassword.length() < 6) {
            throw ApiException.badRequest("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        if (newPassword.length() > 70) {
            throw ApiException.badRequest("La nueva contraseña es demasiado larga.");
        }

        if (newPassword.equals(current)) {
            throw ApiException.badRequest("La nueva contraseña debe ser diferente a la actual.");
        }
    }
}