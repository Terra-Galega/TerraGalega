package com.example.demo.controller;

import com.example.demo.errors.ApiException;
import com.example.demo.dto.LoginRequestDTO;
import com.example.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import java.util.Map;
import com.example.demo.dto.LoginResponseDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    // Servicio encargado de gestionar la autenticación y validar las sesiones.
    @Autowired
    private AuthService authService;

    // Endpoint para iniciar sesión mediante el correo electrónico y la contraseña.
    @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {

        // Delega la validación de las credenciales al servicio de autenticación.
        LoginResponseDTO user = authService.login(request.getEmail(), request.getPassword());

        // Si las credenciales no son válidas, devuelve un error de acceso no autorizado.
        if (user == null) {
            throw ApiException.unauthorized("Correo o contraseña incorrectos.");
        }

        // Retorna los datos del usuario autenticado.
        return user;
    }

    // Endpoint para comprobar si la cuenta asociada a una sesión todavía existe.
    @Operation(summary = "Check that a logged-in account still exists")
    @GetMapping("/session/{type}/{id}")
    public Map<String, Boolean> sessionExists(@PathVariable String type,
            @PathVariable Integer id) {

        // Retorna un mapa con el resultado de la verificación de existencia de la cuenta.
        return Map.of("exists", authService.sessionExists(type, id));
    }
}