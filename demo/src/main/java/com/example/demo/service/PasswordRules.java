package com.example.demo.service;

import com.example.demo.errors.ApiException;

// Contiene las reglas de validación para el cambio de contraseña.
public final class PasswordRules {

    // Evita que se creen instancias de esta clase, ya que solo ofrece métodos estáticos.
    private PasswordRules() {}

    // Comprueba que la contraseña actual sea correcta y que la nueva cumpla las condiciones establecidas.
    public static void validateChange(String stored, String current, String newPassword) {

        // Verifica que ambos campos estén diligenciados y no contengan solo espacios.
        if (current == null || current.isBlank() || newPassword == null || newPassword.isBlank()) {
            throw ApiException.badRequest("Debes completar todos los campos.");
        }

        // Comprueba que la contraseña actual coincida con la almacenada.
        if (!stored.equals(current)) {
            throw ApiException.badRequest("La contraseña actual no es correcta.");
        }

        // Exige que la nueva contraseña tenga al menos 6 caracteres.
        if (newPassword.length() < 6) {
            throw ApiException.badRequest("La nueva contraseña debe tener al menos 6 caracteres.");
        }

        // Limita la longitud máxima de acuerdo con la definición de la columna en la base de datos.
        if (newPassword.length() > 70) { // la columna es length = 70
            throw ApiException.badRequest("La nueva contraseña es demasiado larga.");
        }

        // Evita que el usuario vuelva a establecer la misma contraseña.
        if (newPassword.equals(current)) {
            throw ApiException.badRequest("La nueva contraseña debe ser diferente a la actual.");
        }
    }
}