package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Genera automáticamente los métodos getter y setter.
@Getter
@Setter
// Genera un constructor sin argumentos.
@NoArgsConstructor
// DTO que representa los datos necesarios para solicitar un cambio de contraseña.
public class ChangePasswordRequestDTO {

    // Contiene la contraseña actual del usuario para validar su identidad.
    private String currentPassword;

    // Contiene la nueva contraseña que el usuario desea establecer.
    private String newPassword;
}