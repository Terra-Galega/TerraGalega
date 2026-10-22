package com.example.demo.repository;

import com.example.demo.entities.Admin;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AdminRepository extends JpaRepository<Admin, Integer> {

    // Busca por medio de Email de Admin para evitar duplicado en Register y validar Login
    Admin findByEmail(String email);

}