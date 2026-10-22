package com.example.demo.repository;

import com.example.demo.entities.Client;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    // Busqueda por telefono de CLiente para evitar duplciado en Register y validacion en Login
    Client findByPhone(String phone);

    // Busqueda por Correo de cliente para eviar duplicado en Register y validacion en Login

    Client findByEmail(String email);

}