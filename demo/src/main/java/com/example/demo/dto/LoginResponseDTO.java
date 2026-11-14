package com.example.demo.dto;

import com.example.demo.entities.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

//Clase porque es lo único que el frontend necesita al iniciar sesión. (Auth... User general)
@Getter
// Genera un constructor que recibe todos los atributos.
@AllArgsConstructor
public class LoginResponseDTO {

    // Identificador del usuario que inició sesión.
    private Integer id;

    // Nombre del usuario.
    private String name;

    // Apellido del usuario.
    private String lastName;

    // Rol del usuario, utilizado por el frontend para identificar su tipo de cuenta.
    private Role role;

    // false = cuenta de Google con datos pendientes (ver Client.isProfileComplete)
    private boolean profileComplete;

    // Admins, operadores y domiciliarios siempre tienen el perfil completo
    public LoginResponseDTO(Integer id, String name, String lastName, Role role) {
        this(id, name, lastName, role, true);
    }
}
