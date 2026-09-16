package com.example.demo.entities;

// Rol de la fila dentro de la tabla en la que vive (clients -> CLIENT,
// admins -> ADMIN, etc). Como cada tabla (clients, admins, operators,
// delivery_persons) es completamente independiente y cada fila tiene un
// único valor de "role", es imposible que un mismo registro tenga dos
// roles a la vez.
public enum Role {
    CLIENT,
    ADMIN,
    OPERATOR,
    DELIVERY_PERSON
}