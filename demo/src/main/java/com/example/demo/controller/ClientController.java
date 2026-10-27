package com.example.demo.controller;

import com.example.demo.dto.ChangePasswordRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Collection;

@RestController
@RequestMapping("/clients")
@CrossOrigin(origins = "http://localhost:4200")
public class ClientController {

    // Servicio encargado de gestionar las operaciones relacionadas con los
    // clientes.
    @Autowired
    private ClientService clientService;

    // Endpoint para obtener la lista de todos los clientes registrados.
    @Operation(summary = "Get all clients")
    @GetMapping
    public Collection<Client> getAllClients() {
        return clientService.getAllClients();
    }

    // Endpoint para consultar un cliente específico mediante su identificador.
    @Operation(summary = "Get client by ID")
    @GetMapping("/{id}")
    public Client getClientById(@PathVariable Integer id) {
        return clientService.getClientById(id);
    }

    // Endpoint para registrar un nuevo cliente con los datos recibidos en el cuerpo
    // de la petición.
    @Operation(summary = "Add a new client for registration from client UI")
    @PostMapping
    public Client addClient(@RequestBody Client client) {
        return clientService.addClient(client);
    }

    // Endpoint para actualizar los datos de un cliente existente.
    @Operation(summary = "Update a client by ID from client UI")
    @PutMapping("/{id}")
    public Client updateClient(
            @PathVariable Integer id,
            @RequestBody Client client) {

        return clientService.updateClient(id, client);
    }

    // Endpoint para eliminar un cliente mediante su identificador.
    @Operation(summary = "Delete a client by ID from client UI")
    @DeleteMapping("/{id}")
    public void deleteClient(@PathVariable Integer id) {
        clientService.deleteClient(id);
    }

    // Endpoint para cambiar la contraseña de un cliente utilizando su contraseña
    // actual y la nueva.
    @Operation(summary = "Change password (requires current password)")
    @PutMapping("/{id}/password")
    public void changePassword(@PathVariable Integer id,
            @RequestBody ChangePasswordRequestDTO request) {
        clientService.changePassword(id, request.getCurrentPassword(), request.getNewPassword());
    }
}