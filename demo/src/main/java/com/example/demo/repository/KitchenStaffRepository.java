package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.KitchenStaff;

// Repositorio de las cuentas de cocina. Las búsquedas únicas devuelven null si
// no encuentran nada (igual que DeliveryPersonRepository).
@Repository
public interface KitchenStaffRepository extends JpaRepository<KitchenStaff, Integer> {

    // Se usa en el login y para saber qué cuenta de cocina hace la petición
    KitchenStaff findByEmail(String email);
}
