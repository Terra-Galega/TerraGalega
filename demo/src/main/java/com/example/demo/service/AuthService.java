package com.example.demo.service;

import com.example.demo.dto.LoginResponseDTO;

// Define las operaciones relacionadas con la autenticación y la validación de sesiones.
public interface AuthService {

    // Autentica al usuario mediante su correo electrónico y contraseña.
    LoginResponseDTO login(String email, String password);

    // Comprueba si la cuenta asociada al tipo y al identificador indicados todavía existe.
    boolean sessionExists(String type, Integer id);

}