package com.example.demo.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.demo.entities.Client;
import com.example.demo.entities.User;
import com.example.demo.service.UserService;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    
    private UserService userService;

    private User user;

    @Override
    public User login(String email, String password) {

        user = userService.findByEmail(email);

        if (user == null) {
            throw new IllegalArgumentException("El correo no está registrado.");
        }

        if (!user   .getPassword().equals(password)) {
            throw new IllegalArgumentException("La contraseña es incorrecta.");
        }

        return user;
    }
}
