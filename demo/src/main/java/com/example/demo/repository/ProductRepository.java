package com.example.demo.repository;

import com.example.demo.entities.Product;
import org.springframework.stereotype.Repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByCategoryId(Integer categoryId);

    List<Product> findByPopularTrueAndActiveTrue();

    List<Product> findByCategoryId(Integer categoryId);

    List<Product> findByActiveTrue();
}