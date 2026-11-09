package com.example.demo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.Address;

@Repository
public interface AddressRepository extends JpaRepository<Address, Integer> {

    // Direcciones de un cliente: la predeterminada primero, luego por antigüedad
    List<Address> findByClientIdOrderByDefaultAddressDescIdAsc(Integer clientId);

    // Para evitar direcciones repetidas de un cliente (sin importar mayúsculas)
    Optional<Address> findByClientIdAndLabelIgnoreCase(Integer clientId, String label);

    // Para evitar nombres repetidos de un cliente (sin importar mayúsculas)
    Optional<Address> findByClientIdAndNameIgnoreCase(Integer clientId, String name);
}