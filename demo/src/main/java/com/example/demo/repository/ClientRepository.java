package com.example.demo.repository;

import com.example.demo.entities.Client;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ClientRepository extends JpaRepository<Client, Integer> {

    Client findByEmail(String email);

}