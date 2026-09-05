package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;

import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;

public class AuthServiceImpl implements AuthService {

    @Autowired
    private ClientService clientService;

    private Client client;

    @Override
    public Client login(String email, String password) {

        client = clientService.findByEmail(email);

        if (client == null) {
            throw new IllegalArgumentException("El correo no está registrado.");
        }

        if (!client.getPassword().equals(password)) {
            throw new IllegalArgumentException("La contraseña es incorrecta.");
        }

        return client;
    }
}
