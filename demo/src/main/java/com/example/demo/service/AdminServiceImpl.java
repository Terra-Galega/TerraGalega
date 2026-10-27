package com.example.demo.service;

import com.example.demo.errors.ApiException;
import com.example.demo.entities.Admin;
import com.example.demo.repository.AdminRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;

// Implementa las operaciones definidas en AdminService.
@Service
public class AdminServiceImpl implements AdminService {

    // Repositorio encargado de acceder a los datos de los administradores.
    @Autowired
    private AdminRepository repository;

    // Obtiene todos los administradores registrados en el repositorio.
    @Override
    @Transactional
    public Collection<Admin> getAllAdmins() {
        return repository.findAll();
    }

    // Busca un administrador por su ID y lanza una excepción si no existe.
    @Override
    @Transactional
    public Admin getAdminById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El admin no existe."));
    }

    // Valida el correo electrónico antes de guardar el nuevo administrador.
    @Override
    @Transactional
    public Admin addAdmin(Admin admin) {
        EmailRules.validate(admin.getEmail());
        return repository.save(admin);
    }

    // Busca y devuelve el administrador asociado al correo electrónico proporcionado.
    @Override
    @Transactional
    public Admin findByEmail(String email) {
        return repository.findByEmail(email);
    }

}