package com.example.demo.service;

import com.example.demo.entities.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
public class ProductServiceImpl implements ProductService {
    
    @Autowired
    private ProductRepository repository;

    @Override
    public Collection<Product> getAllProducts() {
        return repository.findAll();
    }

    @Override
    public Product getProductById(Integer id) {
        return repository.findById(id);
    }
}