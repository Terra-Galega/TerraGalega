package com.example.demo.service;

public final class PasswordRules {

    private PasswordRules() {}

    public static void validateChange(String stored, String current, String newPassword) {
        if (current == null || current.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("Debes completar todos los campos.");
        }
        if (!stored.equals(current)) {
            throw new IllegalArgumentException("La contraseña actual no es correcta.");
        }
        if (newPassword.length() < 6) {
            throw new IllegalArgumentException("La nueva contraseña debe tener al menos 6 caracteres.");
        }
        if (newPassword.length() > 70) { // la columna es length = 70
            throw new IllegalArgumentException("La nueva contraseña es demasiado larga.");
        }
        if (newPassword.equals(current)) {
            throw new IllegalArgumentException("La nueva contraseña debe ser diferente a la actual.");
        }
    }
}