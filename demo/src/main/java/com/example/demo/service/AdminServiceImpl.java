package com.example.demo.service;

import com.example.demo.entities.Admin;
import com.example.demo.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

@Service
public class AdminServiceImpl implements AdminService {

    @Autowired
    private AdminRepository repository;

    @Override
    @Transactional
    public Collection<Admin> getAllAdmins() {
        return repository.findAll();
    }

    @Override
    @Transactional
    public Admin getAdminById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El admin no existe."));
    }

    @Override
    @Transactional
    public Admin addAdmin(Admin admin) {
        return repository.save(admin);
    }

    @Override
    @Transactional
    public Admin findByEmail(String email) {
        return repository.findByEmail(email);
    }

}