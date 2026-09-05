package com.example.demo.service;

import com.example.demo.entities.Client;

public interface AuthService {

    Client login(String email, String password);

}
