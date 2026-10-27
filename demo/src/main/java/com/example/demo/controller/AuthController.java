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

    @Autowired
    private AuthService authService;

        @Operation(summary = "Login with email and password")
    @PostMapping("/login")
    public LoginResponseDTO login(@RequestBody LoginRequestDTO request) {

        LoginResponseDTO user = authService.login(request.getEmail(), request.getPassword());

        if (user == null) {
            throw ApiException.unauthorized("Correo o contraseña incorrectos.");
        }

        return user;
    }

    @Operation(summary = "Check that a logged-in account still exists")
    @GetMapping("/session/{type}/{id}")
    public Map<String, Boolean> sessionExists(@PathVariable String type,
            @PathVariable Integer id) {
        return Map.of("exists", authService.sessionExists(type, id));
    }
}