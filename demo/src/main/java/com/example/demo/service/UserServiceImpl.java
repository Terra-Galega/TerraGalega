package com.example.demo.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.entities.User;
import com.example.demo.repository.UserRepository;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository repository;

    @Override
    public Collection<User> getAllUsers() {
        return repository.findAll();
    }

    @Override
    public User getUserById(Integer id) {

        User user = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe un usuario con este id."));

        if (user == null) {
            throw new RuntimeException("El usuario no existe");
        }

        return user;
    }

    @Override
    public User findByEmail(String email) {
        User user = repository.findByEmail(email);

        if (user == null) {
            throw new RuntimeException("El usuario no existe");
        }

        return user;
    }

}
