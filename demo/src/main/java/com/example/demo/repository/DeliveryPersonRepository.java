package com.example.demo.repository;

import com.example.demo.entities.DeliveryPerson;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface DeliveryPersonRepository extends JpaRepository<DeliveryPerson, Integer> {

    DeliveryPerson findByEmail(String email);

}