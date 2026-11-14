package com.example.demo.dto;

// Cuerpo de PUT /clients/me/complete-profile: lo que Google no da y el
// registro normal sí pide. address solo hace falta si aún no tiene ninguna.
public record CompleteProfileDTO(
        String name,
        String lastName,
        String phone,
        AddressDTO address) {
}
