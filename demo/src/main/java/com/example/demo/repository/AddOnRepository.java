package com.example.demo.repository;

import com.example.demo.entities.AddOn;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface AddOnRepository extends JpaRepository<AddOn, Integer> {

    // Busqueda por (active =true) -> Devuelve adicionales activos
    List<AddOn> findByActiveTrue();
    
    // Devuelve los adicionales activos y los cuales pertenecen a un cateogoria - reciben id de categoria
    List<AddOn> findByActiveTrueAndCategories_Id(Integer categoryId);
}