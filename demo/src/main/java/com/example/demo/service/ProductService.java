package com.example.demo.service;

import com.example.demo.dto.ProductOptionDTO;
import com.example.demo.entities.Product;
import java.util.Collection;
import java.util.List;

/**
 * Contrato de la lógica de negocio de los productos (platos de la carta).
 * La implementación está en ProductServiceImpl.
 */
public interface ProductService {
    // Todos los productos, activos e inactivos (vista del admin)
    Collection<Product> getAllProducts();

    // Solo los productos activos (los que ve el cliente)
    Collection<Product> getAllProductsActive();

    // Un producto por id; falla si no existe
    Product getProductById(Integer id);

    // Devuelve el producto solo si está activo
    Product getActiveProductById(Integer id);

    // Devuelve los productos marcados como populares, usados en "Favoritos de la
    // casa" (home)
    Collection<Product> getPopularProductsActive();

    // Devuelve hasta 3 productos de la misma categoría que el producto indicado
    // (para "También te puede gustar" en /menu/{id})
    Collection<Product> getRelatedProducts(Integer id);

    // Operaciones para el panel de administración (CRUD)
    Product addProduct(Product product);

    Product updateProduct(Integer id, Product product);

    void deleteProduct(Integer id);

    Product toggleProductActive(Integer id);

    // Ids de los productos activos, sin traer los productos completos
    List<Integer> getActiveProductIds();

    // Solo id y nombre de todos los productos, para desplegables de filtro
    List<ProductOptionDTO> getProductOptions();
}