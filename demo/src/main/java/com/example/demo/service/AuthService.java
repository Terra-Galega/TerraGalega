package com.example.demo.service;

public interface AuthService {

    Object login(String email, String password);

    boolean sessionExists(String type, Integer id);

}