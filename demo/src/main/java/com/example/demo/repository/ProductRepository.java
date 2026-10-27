package com.example.demo.repository;

import com.example.demo.dto.ProductOptionDTO;
import com.example.demo.entities.Product;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a datos de los productos. Al extender JpaRepository ya incluye las
 * operaciones básicas (guardar, buscar por id, listar, borrar). Los métodos
 * derivados generan su SQL a partir del nombre; los que llevan @Query
 * definen la consulta a mano.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Integer> {

    // true si hay al menos un producto en esa categoría (para impedir borrarla)
    boolean existsByCategoryId(Integer categoryId);

    // Productos populares que además están activos
    List<Product> findByPopularTrueAndActiveTrue();

    // Todos los productos de una categoría
    List<Product> findByCategoryId(Integer categoryId);

    // Solo los productos activos
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