package com.example.demo.service;

import com.example.demo.dto.LoginResponseDTO;

public interface AuthService {

    LoginResponseDTO login(String email, String password);

    boolean sessionExists(String type, Integer id);

}