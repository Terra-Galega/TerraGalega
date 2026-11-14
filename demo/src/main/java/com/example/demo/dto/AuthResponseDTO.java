package com.example.demo.dto;

import com.example.demo.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

// Respuesta del login, del registro y de la edición del perfil: el JWT y los
// datos básicos para que el front muestre el nombre y sepa a qué página ir.
// Nunca incluye la contraseña.
@Getter
@AllArgsConstructor
public class AuthResponseDTO {

    // El front lo envía en cada petición en el header Authorization
    private String token;

    // Id del perfil (cliente, admin u operador), no el de la tabla users
    private Integer id;

    private String name;

    private String lastName;

    private Role role;

    // false = cuenta creada con Google a la que le faltan datos (teléfono,
    // apellido o dirección). El front la lleva a "Completa tu perfil".
    private boolean profileComplete;

    // Para cuentas que siempre tienen el perfil completo
    public AuthResponseDTO(String token, Integer id, String name, String lastName, Role role) {
        this(token, id, name, lastName, role, true);
    }

    public AuthResponseDTO(String token, LoginResponseDTO profile) {
        this(token, profile.getId(), profile.getName(), profile.getLastName(),
                profile.getRole(), profile.isProfileComplete());
    }
}
