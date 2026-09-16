package com.example.demo.entities;

// Roles posibles de un usuario dentro del sistema.
// Cada fila de UserRole tiene exactamente un valor de este enum,
// por lo que un mismo usuario nunca puede tener más de un rol a la vez.
public enum Role {
    CLIENT,
    ADMIN,
    OPERATOR,
    DELIVERY_PERSON
}