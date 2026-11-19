package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.Waiter;

// Repositorio de meseros. Las búsquedas únicas devuelven null si no encuentran
// nada (igual que DeliveryPersonRepository).
@Repository
public interface WaiterRepository extends JpaRepository<Waiter, Integer> {

    // Se usa en el login y para saber qué mesero hace la petición
    Waiter findByEmail(String email);
}
