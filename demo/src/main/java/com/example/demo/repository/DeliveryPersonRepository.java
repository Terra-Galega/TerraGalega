package com.example.demo.repository;

import java.util.List;

import com.example.demo.entities.DeliveryPerson;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Integer> {

    //Busca un deliveryPerson por email
    DeliveryPerson findByEmail(String email);

    // Busqueda de un delivery person que este activo 
    List<DeliveryPerson> findByAvailableTrueAndActiveTrue();

    //Busqueda por Id del deliveryPerson, evitar registro con misma identificación
    DeliveryPerson findByIdentification(String identification);

    // Busqueda por telefono del deliveryPerso, para evitar duplicados
    DeliveryPerson findByPhone(String phone);
}