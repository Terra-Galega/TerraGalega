package com.example.demo.repository;

import com.example.demo.entities.Operator;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface OperatorRepository extends JpaRepository<Operator, Integer> {


    // BUsqueda de operador por email para evitar duplicado en Register y validacion en Login
    Operator findByEmail(String email);

    // Busqueda de operador por usuario para evitar duplicado en Register y validacion alterna en Login
    Operator findByUsername(String username);

}