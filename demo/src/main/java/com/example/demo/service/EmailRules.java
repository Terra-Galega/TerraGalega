package com.example.demo.service;

import com.example.demo.errors.ApiException;

public final class EmailRules {

    private EmailRules() {
    }

    // Algo antes del @, un solo @, algo después del @ y sin espacios
    public static void validate(String email) {
        if (email == null || !email.matches("[^\\s@]+@[^\\s@]+")) {
            throw ApiException.badRequest("El correo electrónico no es válido.");
        }
    }
}