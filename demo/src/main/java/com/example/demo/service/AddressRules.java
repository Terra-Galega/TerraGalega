package com.example.demo.service;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import com.example.demo.errors.ApiException;

// Reglas de las direcciones, compartidas por el registro y el CRUD de direcciones.
public final class AddressRules {

    // Mismo máximo que Address.label y Order.address
    public static final int LABEL_MAX = 120;

    // Máximo del nombre de la dirección
    public static final int NAME_MAX = 40;

    // Nombre que se asigna cuando el cliente no escribe uno
    public static final String DEFAULT_NAME = "Casa";

    private AddressRules() {
    }

    // La dirección es obligatoria y tiene un máximo de caracteres.
    // Devuelve el texto ya sin espacios sobrantes.
    public static String clean(String label) {
        if (label == null || label.trim().isEmpty()) {
            throw ApiException.badRequest("Escribe la dirección.");
        }
        String clean = label.trim();
        if (clean.length() > LABEL_MAX) {
            throw ApiException.badRequest(
                    "La dirección no puede superar " + LABEL_MAX + " caracteres.");
        }
        return clean;
    }

    // Devuelve el nombre sin espacios sobrantes, o null si no escribió ninguno
    // (en ese caso se asigna uno por defecto con freeName).
    public static String cleanName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        String clean = name.trim();
        if (clean.length() > NAME_MAX) {
            throw ApiException.badRequest(
                    "El nombre no puede superar " + NAME_MAX + " caracteres.");
        }
        return clean;
    }

    // "Casa", o "Casa 2", "Casa 3"... si esos nombres ya están usados.
    public static String freeName(Collection<String> taken) {
        Set<String> used = taken.stream()
                .map(n -> n.trim().toLowerCase())
                .collect(Collectors.toSet());

        if (!used.contains(DEFAULT_NAME.toLowerCase())) {
            return DEFAULT_NAME;
        }

        int number = 2;
        while (used.contains((DEFAULT_NAME + " " + number).toLowerCase())) {
            number++;
        }
        return DEFAULT_NAME + " " + number;
    }
}