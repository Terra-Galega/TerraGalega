package com.example.demo.service;

import java.util.Collection;

import com.example.demo.entities.User;

public interface UserService {

    Collection<User> getAllUsers(); 

    User getUserById(Integer id);

    User findByEmail(String email);

}
