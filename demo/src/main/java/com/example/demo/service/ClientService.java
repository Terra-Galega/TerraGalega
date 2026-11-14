package com.example.demo.service;

import com.example.demo.entities.Client;

import java.util.Collection;

// Define las operaciones disponibles para la gestión de clientes.
public interface ClientService {

    // Obtiene la colección de todos los clientes registrados.
    Collection<Client> getAllClients();

    // Busca un cliente por su identificador.
    Client getClientById(Integer id);

    // Registra un nuevo cliente.
    Client addClient(Client client);

    // Actualiza los datos de un cliente existente.
    Client updateClient(Integer id, Client client);

    // Cuenta creada con Google: guarda apellido, teléfono y la primera dirección
    Client completeProfile(Integer id, com.example.demo.dto.CompleteProfileDTO data);

    // Elimina un cliente por su identificador.
    void deleteClient(Integer id);

    // Busca un cliente a partir de su correo electrónico.
    Client findByEmail(String email);

    // Cambia la contraseña de un cliente validando sus credenciales actuales.
    void changePassword(Integer id, String currentPassword, String newPassword);
}