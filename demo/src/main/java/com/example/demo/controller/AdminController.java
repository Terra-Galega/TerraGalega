package com.example.demo.controller;

import com.example.demo.entities.Product;
import com.example.demo.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;

import java.util.Collection;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Operation(summary = "Get all products (only for admin)")
    @GetMapping
    public Collection<Product> getAllProducts() {
        return productService.getAllProducts();
    }

    @Operation(summary = "Get all active products (only for admin)")
    @GetMapping("/active")
    public Collection<Product> getActiveProducts() {
        return productService.getAllProductsActive();
    }

    @Operation(summary = "Get product by ID (only for admin)")
    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Integer id) {
        return productService.getProductById(id);
    }

    @Operation(summary = "Get popular products (only for admin)")
    @GetMapping("/popular")
    public Collection<Product> getPopularProducts() {
        return productService.getPopularProductsActive();
    }

    @Operation(summary = "Add a new product (only for admin)")
    @PostMapping
    public Product addProduct(@RequestBody Product product) {
        return productService.addProduct(product);
    }

    @Operation(summary = "Update a product (only for admin)")
    @PutMapping("/{id}")
    public Product updateProduct(
            @PathVariable Integer id,
            @RequestBody Product product) {

        return productService.updateProduct(id, product);
    }

    @Operation(summary = "Delete a product (only for admin)")
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
    }

    @Operation(summary = "Toggle product active status (only for admin)")
    @PutMapping("/{id}/toggle")
    public Product toggleProduct(@PathVariable Integer id) {
        return productService.toggleProductActive(id);
    }
}