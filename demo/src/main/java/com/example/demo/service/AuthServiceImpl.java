package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.entities.UserRole;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserRoleService userRoleService;

    @Override
    public UserRole login(String email, String password) {

        UserRole userRole = userRoleService.findByEmail(email);

        if (userRole == null) {
            throw new IllegalArgumentException("El correo no está registrado.");
        }

        if (!userRole.getPassword().equals(password)) {
            throw new IllegalArgumentException("La contraseña es incorrecta.");
        }

        return userRole;
    }
}