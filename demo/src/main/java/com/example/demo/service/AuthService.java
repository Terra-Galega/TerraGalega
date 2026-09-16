package com.example.demo.service;

import com.example.demo.entities.UserRole;

public interface AuthService {

    UserRole login(String email, String password);

}