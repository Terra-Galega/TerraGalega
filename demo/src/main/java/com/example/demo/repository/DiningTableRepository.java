package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.DiningTable;

// Solo lectura en la práctica: las mesas no tienen CRUD, las carga DiningTableLoader.
@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {

    // Pedidos de mesa: las mesas en servicio de un piso, por número
    java.util.List<DiningTable> findByFloorAndAvailableTrueOrderByIdAsc(Integer floor);
}
