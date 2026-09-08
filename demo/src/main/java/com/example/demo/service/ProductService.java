package com.example.demo.service;

import com.example.demo.entities.Product;
import java.util.Collection;

public interface ProductService {
    Collection<Product> getAllProducts();
    Collection<Product> getAllProductsActive();
    Product getProductById(Integer id);
    //Devuelve los productos marcados como populares, usados en "Favoritos de la casa" (home)
    Collection<Product> getPopularProductsActive();
    //Devuelve hasta 3 productos de la misma categoría que el producto indicado (para "También te puede gustar" en /menu/{id})
    Collection<Product> getRelatedProducts(Integer id);

    //Operaciones para el panel de administración (CRUD)
    Product addProduct(Product product);
    Product updateProduct(Integer id, Product product);
    void deleteProduct(Integer id);
    Product toggleProductActive(Integer id);
}