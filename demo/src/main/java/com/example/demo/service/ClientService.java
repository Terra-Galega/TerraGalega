package com.example.demo.service;

import com.example.demo.entities.Client;
import java.util.Collection;

public interface ClientService {
    Collection<Client> getAllClients();

    Client getClientById(Integer id);

    Client addClient(Client Client);

    Client updateClient(Integer id, Client Client);

    void deleteClient(Integer id);

    // Login: devuelve el Client si el email y la contraseña coinciden, o null si no
    Client login(String email, String password);
}