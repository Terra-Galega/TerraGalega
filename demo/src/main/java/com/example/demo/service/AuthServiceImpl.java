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
        //
        // Devuelve null (no lanza excepción) cuando el correo no existe o la
        // contraseña no coincide: AuthController.doLogin() espera null para
        // mostrar "Correo o contraseña incorrectos." en el propio formulario.
        // Si esto lanzara, la excepción quedaría sin capturar (no está en
        // GlobalExceptionHandler) y el login fallido terminaría en la
        // página blanca de error de Spring en vez de en el mensaje del form.
        Client client = clientService.findByEmail(email);

        if (client != null) {
            return client.getPassword().equals(password) ? client : null;
        }

        Admin admin = adminService.findByEmail(email);

        if (admin != null) {
            return admin.getPassword().equals(password) ? admin : null;
        }

        return null;
    }
}