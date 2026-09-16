package com.example.demo.service;

import java.util.Collection;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entities.UserRole;
import com.example.demo.repository.UserRoleRepository;

@Service
public class UserRoleServiceImpl implements UserRoleService {

    @Autowired
    private UserRoleRepository repository;

    @Override
    @Transactional
    public Collection<UserRole> getAllUserRoles() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public UserRole getUserRoleById(Integer id) {

        UserRole userRole = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No existe un usuario con este id."));

        if (userRole == null) {
            throw new RuntimeException("El usuario no existe");
        }

        return userRole;
    }

    @Override
    @Transactional
    public UserRole findByEmail(String email) {
        UserRole userRole = repository.findByEmail(email);

        if (userRole == null) {
            throw new RuntimeException("El usuario no existe");
        }

        return userRole;
    }

}