package com.example.demo.repository;

import com.example.demo.entities.Category;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {

    // Busca una categoria por el nombre de la misma
    Category findByName(String name);

}