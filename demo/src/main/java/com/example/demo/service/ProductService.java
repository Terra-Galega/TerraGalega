package com.example.demo.service;

import com.example.demo.entities.Product;
import java.util.Collection;

public interface ProductService {
    Collection<Product> getAllProducts();
    Product getProductById(Integer id);
}