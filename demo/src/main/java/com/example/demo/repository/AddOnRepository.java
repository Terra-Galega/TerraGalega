package com.example.demo.repository;

import com.example.demo.entities.AddOn;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de los adicionales. Al extender JpaRepository ya incluye las
 * operaciones básicas (guardar, buscar por id, listar, borrar). Los métodos
 * de abajo son consultas derivadas: Spring genera el SQL a partir del nombre.
 */
@Repository
public interface AddOnRepository extends JpaRepository<AddOn, Integer> {

    // Busqueda por (active =true) -> Devuelve adicionales activos
    List<AddOn> findByActiveTrue();

    // Adicionales que tienen asociada una categoría (sin importar si están activos)
    List<AddOn> findByCategories_Id(Integer categoryId);

    // Devuelve los adicionales activos y los cuales pertenecen a un cateogoria -
    // reciben id de categoria
    List<AddOn> findByActiveTrueAndCategories_Id(Integer categoryId);
}