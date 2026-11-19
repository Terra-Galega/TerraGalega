package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.DiningTable;

// Solo lectura en la práctica: las mesas no tienen CRUD, las carga DiningTableLoader.
@Repository
public interface DiningTableRepository extends JpaRepository<DiningTable, Integer> {

    // Pedidos de mesa: todas las mesas de un piso (libres y ocupadas), por número
    java.util.List<DiningTable> findByFloorOrderByIdAsc(Integer floor);

    // Bloquea la mesa mientras se toma un pedido: si dos meseros la toman a la
    // vez, el segundo espera y ya ve que la atiende el primero
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select t from DiningTable t where t.id = :id")
    java.util.Optional<DiningTable> lockById(@org.springframework.data.repository.query.Param("id") Integer id);
}
