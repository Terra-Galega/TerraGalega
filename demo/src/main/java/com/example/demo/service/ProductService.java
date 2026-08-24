package com.example.demo.service;

import com.example.demo.entities.Product;
import java.util.Collection;

public interface ProductService {
    Collection<Product> getAllProducts();
    Product getProductById(Integer id);
    //Devuelve los productos marcados como populares, usados en "Favoritos de la casa" (home)
    Collection<Product> getPopularProducts();

    //── Operaciones para el panel de administración (/admin) ──
    Product addProduct(Product product);
    Product updateProduct(Integer id, Product product);
    void deleteProduct(Integer id);
    Product toggleProductActive(Integer id);
}