package com.example.demo.repository;

import com.example.demo.dto.ProductOptionDTO;
import com.example.demo.entities.Product;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    boolean existsByCategoryId(Integer categoryId);

    List<Product> findByPopularTrueAndActiveTrue();

    List<Product> findByCategoryId(Integer categoryId);

    List<Product> findByActiveTrue();

    // Hasta 3 productos activos de la misma categoría, sin contar el propio
    List<Product> findTop3ByCategoryIdAndActiveTrueAndIdNotOrderByIdAsc(
            Integer categoryId, Integer id);

    // Solo los ids de los productos activos (para validar el carrito)
    @Query("select p.id from Product p where p.active = true")
    List<Integer> findActiveIds();

    // Solo id y nombre de todos los productos (desplegable de filtro del admin)
    @Query("select new com.example.demo.dto.ProductOptionDTO(p.id, p.name) from Product p")
    List<ProductOptionDTO> findOptions();
}