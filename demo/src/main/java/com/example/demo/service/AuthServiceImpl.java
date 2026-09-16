package com.example.demo.service;

import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private ClientService clientService;

    @Autowired
    private AdminService adminService;

    @Override
    public Object login(String email, String password) {

        // Como ya no existe una tabla única de usuarios, probamos el correo
        // contra cada tabla (clients, admins) hasta encontrar coincidencia.
        Client client = clientService.findByEmail(email);

        if (client != null) {
            if (!client.getPassword().equals(password)) {
                throw new IllegalArgumentException("La contraseña es incorrecta.");
            }
            return client;
        }

        Admin admin = adminService.findByEmail(email);

        if (admin != null) {
            if (!admin.getPassword().equals(password)) {
                throw new IllegalArgumentException("La contraseña es incorrecta.");
            }
            return admin;
        }

        throw new IllegalArgumentException("El correo no está registrado.");
    }
}