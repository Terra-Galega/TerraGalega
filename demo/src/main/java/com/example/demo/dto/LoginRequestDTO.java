package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Genera automáticamente los métodos getter y setter.
@Getter
@Setter
// Genera un constructor sin argumentos.
@NoArgsConstructor
// DTO que representa los datos enviados por el usuario para iniciar sesión.
public class LoginRequestDTO {

    // Correo electrónico utilizado para identificar la cuenta.
    private String email;

    // Contraseña asociada a la cuenta para validar el acceso.
    private String password;
}