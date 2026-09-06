package com.example.demo.service;

import com.example.demo.entities.Product;
import com.example.demo.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.stream.Collectors;

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

        Product product = repository.findById(id).orElseThrow();

        if (product == null) {
            throw new RuntimeException("El producto no existe");
        }

        return product;
    }

    @Override
    public Collection<Product> getPopularProducts() {
        return repository.findByPopularTrue();
    }

    @Override
    public Collection<Product> getRelatedProducts(Integer id) {
        Product product = repository.findById(id).orElseThrow();
        if (product == null) {
            return java.util.List.of();
        }
        // Filtra los productos de la misma categoría (excluyendo el propio producto) y
        // toma los primeros 3
        return repository.findAll().stream()
                .filter(p -> p.getCategory().equals(product.getCategory()) && !p.getId().equals(product.getId()))
                .limit(3)
                .collect(Collectors.toList());
    }

    @Override
    public Product addProduct(Product product) {
        return repository.save(product);
    }

    @Override
    public Product updateProduct(Integer id, Product product) {

        Product existingProduct = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        product.setId(id);

        return repository.save(product);
    }

    @Override
    public void deleteProduct(Integer id) {
        repository.deleteById(id);
    }

    @Override
    public Product toggleProductActive(Integer id) {
        Product product = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        product.setActive(!product.getActive());

        return repository.save(product);
    }
}