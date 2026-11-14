package com.example.demo.controller;

import java.util.Collection;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.dto.AuthResponseDTO;
import com.example.demo.dto.ChangePasswordRequestDTO;
import com.example.demo.dto.ClientProfileDTO;
import com.example.demo.dto.ClientUpdateDTO;
import com.example.demo.dto.CompleteProfileDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.security.JwtGenerator;
import com.example.demo.service.ClientService;

import io.swagger.v3.oas.annotations.Operation;

// El registro de clientes se hace en /auth/signup.
@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Autowired
    private JwtGenerator jwtGenerator;

    // =========================
    // ADMIN
    // =========================

    @Operation(summary = "Get all clients (admin)")
    @GetMapping
    public Collection<ClientProfileDTO> getAllClients() {
        return clientService.getAllClients().stream().map(ClientProfileDTO::from).toList();
    }

    @Operation(summary = "Get client by ID (admin)")
    @GetMapping("/{id}")
    public ClientProfileDTO getClientById(@PathVariable Integer id) {
        return ClientProfileDTO.from(clientService.getClientById(id));
    }

    // =========================
    // CLIENTE CON SESIÓN
    // El cliente se obtiene del token, nunca de un id en la URL,
    // así nadie puede consultar o modificar la cuenta de otra persona.
    // =========================

    @Operation(summary = "Get the logged-in client")
    @GetMapping("/me")
    public ClientProfileDTO getMe() {
        return ClientProfileDTO.from(authenticatedUser.client());
    }

    // Devuelve un token nuevo, porque si el cliente cambió su correo el anterior deja de servir
    @Operation(summary = "Update the logged-in client and return a fresh JWT")
    @PutMapping("/me")
    public AuthResponseDTO updateMe(@RequestBody ClientUpdateDTO request) {
        Integer id = authenticatedUser.client().getId();
        Client saved = clientService.updateClient(id, request.toEntity());

        String token = jwtGenerator.generateToken(saved.getEmail(), List.of(Role.CLIENT.name()));
        return new AuthResponseDTO(token, saved.getId(), saved.getName(), saved.getLastName(), saved.getRole(),
                saved.isProfileComplete());
    }

    // Cuentas creadas con Google: guarda lo que Google no da (apellido,
    // teléfono y la primera dirección) con las mismas reglas del registro
    @Operation(summary = "Complete the profile of an account created with Google")
    @PutMapping("/me/complete-profile")
    public AuthResponseDTO completeProfile(@RequestBody CompleteProfileDTO request) {
        Integer id = authenticatedUser.client().getId();
        Client saved = clientService.completeProfile(id, request);

        String token = jwtGenerator.generateToken(saved.getEmail(), List.of(Role.CLIENT.name()));
        return new AuthResponseDTO(token, saved.getId(), saved.getName(), saved.getLastName(), saved.getRole(),
                saved.isProfileComplete());
    }

    @Operation(summary = "Delete the logged-in client account")
    @DeleteMapping("/me")
    public void deleteMe() {
        clientService.deleteClient(authenticatedUser.client().getId());
    }

    @Operation(summary = "Change the logged-in client's password (requires current password)")
    @PutMapping("/me/password")
    public void changeMyPassword(@RequestBody ChangePasswordRequestDTO request) {
        clientService.changePassword(authenticatedUser.client().getId(),
                request.getCurrentPassword(), request.getNewPassword());
    }
}
