package com.example.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// Cuerpo de POST /auth/resend-verification: el correo de la cuenta sin confirmar
@Getter
@Setter
@NoArgsConstructor
public class ResendVerificationRequestDTO {

    private String email;
}
