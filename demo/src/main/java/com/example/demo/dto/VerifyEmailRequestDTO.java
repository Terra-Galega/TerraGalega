package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Cuerpo de POST /auth/verify-email: el token que venía en el enlace del correo
@Getter
@Setter
@NoArgsConstructor
public class VerifyEmailRequestDTO {

    private String token;
}
