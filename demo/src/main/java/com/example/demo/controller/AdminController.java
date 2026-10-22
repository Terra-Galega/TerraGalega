package com.example.demo.controller;

import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.ProductDTO;
import com.example.demo.entities.Category;
import com.example.demo.entities.Product;
import com.example.demo.service.CategoryService;
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

    @Autowired
    private CategoryService categoryService;

    private ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();

        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setImageUrl(product.getImageUrl());
        dto.setActive(product.getActive());
        dto.setPopular(product.getPopular());
        dto.setVegetarian(product.isVegetarian());
        dto.setSpicyMild(product.isSpicyMild());
        dto.setSpicyHot(product.isSpicyHot());
        dto.setContainsNuts(product.isContainsNuts());
        dto.setContainsSeafood(product.isContainsSeafood());
        dto.setContainsGluten(product.isContainsGluten());

        if (product.getCategory() != null) {
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(product.getCategory().getId());
            categoryDTO.setName(product.getCategory().getName());
            categoryDTO.setDescription(product.getCategory().getDescription());
            dto.setCategory(categoryDTO);
        }

        return dto;
    }

    private Product toEntity(ProductDTO dto) {
        Product product = new Product();

        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setImageUrl(dto.getImageUrl());
        product.setActive(dto.getActive());
        product.setPopular(dto.getPopular());
        product.setVegetarian(dto.isVegetarian());
        product.setSpicyMild(dto.isSpicyMild());
        product.setSpicyHot(dto.isSpicyHot());
        product.setContainsNuts(dto.isContainsNuts());
        product.setContainsSeafood(dto.isContainsSeafood());
        product.setContainsGluten(dto.isContainsGluten());

        if (dto.getCategory() != null
                && dto.getCategory().getId() != null) {
            Category category = categoryService
                    .getCategoryById(dto.getCategory().getId());

            product.setCategory(category);
        }

        return product;
    }

    @Operation(summary = "Get all products")
    @GetMapping
    public Collection<ProductDTO> getAllProducts() {
        return productService.getAllProducts()
                .stream().map(this::toDTO).toList();
    }

    @Operation(summary = "Get all active products")
    @GetMapping("/active")
    public Collection<ProductDTO> getActiveProducts() {
        return productService.getAllProductsActive()
                .stream().map(this::toDTO).toList();
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable Integer id) {
        return toDTO(productService.getProductById(id));
    }

    @Operation(summary = "Get popular products")
    @GetMapping("/popular")
    public Collection<ProductDTO> getPopularProducts() {
        return productService.getPopularProductsActive()
                .stream().map(this::toDTO).toList();
    }

    @Operation(summary = "Add a new product")
    @PostMapping
    public ProductDTO addProduct(@RequestBody ProductDTO dto) {
        return toDTO(productService.addProduct(toEntity(dto)));
    }

    @Operation(summary = "Update a product")
    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable Integer id,
            @RequestBody ProductDTO dto) {
        return toDTO(productService.updateProduct(id, toEntity(dto)));
    }

    @Operation(summary = "Delete a product")
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
    }

    @Operation(summary = "Toggle product active status")
    @PutMapping("/{id}/toggle")
    public ProductDTO toggleProduct(@PathVariable Integer id) {
        return toDTO(productService.toggleProductActive(id));
    }
}