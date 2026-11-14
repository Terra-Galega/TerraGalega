package com.example.demo.service;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.LoginResponseDTO;

// Operaciones de autenticación
public interface AuthService {

    // Valida correo y contraseña. Si son correctos devuelve el JWT y los datos básicos;
    // si no, responde 401 (o 403 si la cuenta está desactivada).
    AuthResponseDTO login(String email, String password);

    // Datos básicos (id, nombre, apellido y rol) del perfil que tiene ese correo
    LoginResponseDTO currentUser(String email);
}
