package com.example.demo.service;

import com.example.demo.entities.User;

public interface AuthService {

    User login(String email, String password);

}
