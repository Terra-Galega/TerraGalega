package com.example.demo.repository;

import com.example.demo.entities.Category;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de las categorías. Al extender JpaRepository ya incluye las
 * operaciones básicas (guardar, buscar por id, listar, borrar).
 */
@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Busca una categoria por el nombre de la misma (devuelve null si no existe).
    // Se usa para validar que el nombre no se repita.
    Category findByName(String name);

}