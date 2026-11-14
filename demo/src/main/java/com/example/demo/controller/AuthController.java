package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.dto.LoginResponseDTO;
import com.example.demo.dto.SignupRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.AuthService;
import com.example.demo.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // Devuelve el JWT y los datos básicos si el correo y la contraseña son correctos
    @Operation(summary = "Login with email and password, returns a JWT")
    @PostMapping("/login")
    public AuthResponseDTO login(@RequestBody LoginRequestDTO request) {
        return authService.login(request.getEmail(), request.getPassword());
    }

    // Registra un cliente y lo deja con la sesión iniciada
    @Operation(summary = "Register a new client and return a JWT")
    @PostMapping("/signup")
    public AuthResponseDTO signup(@RequestBody SignupRequestDTO request) {
        Client saved = clientService.addClient(request.toEntity());
        return authService.login(saved.getEmail(), request.getPassword());
    }

    // Datos del usuario con sesión iniciada, según el token que envía
    @Operation(summary = "Get the logged-in user from the JWT")
    @GetMapping("/me")
    public LoginResponseDTO me() {
        return authService.currentUser(authenticatedUser.email());
    }
}
