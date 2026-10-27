package com.example.demo.service;

import com.example.demo.entities.Admin;

import java.util.Collection;

// Define las operaciones disponibles para gestionar los administradores.
public interface AdminService {

    // Obtiene la colección de administradores registrados.
    Collection<Admin> getAllAdmins();

    // Busca un administrador por su identificador.
    Admin getAdminById(Integer id);

    // Registra un nuevo administrador.
    Admin addAdmin(Admin admin);

    // Busca un administrador mediante su correo electrónico.
    Admin findByEmail(String email);

}