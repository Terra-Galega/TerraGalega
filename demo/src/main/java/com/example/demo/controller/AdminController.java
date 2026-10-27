package com.example.demo.controller;

import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.CategoryRefDTO;
import com.example.demo.dto.ClientDTOAdmin;
import com.example.demo.dto.ProductDTO;
import com.example.demo.dto.ProductOptionDTO;
import com.example.demo.dto.ProductSummaryDTO;
import com.example.demo.entities.Category;
import com.example.demo.entities.Product;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ClientService;
import com.example.demo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/products")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ClientService clientService;

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
    
    @Operation(summary = "Get clients but only atributes: id, name, email, phone")
    @GetMapping("/clients")
    public Collection<ClientDTOAdmin> getClientDTOAdmins() {
        return clientService.getAllClients()
                .stream()
                .map(client -> {
                    ClientDTOAdmin dto = new ClientDTOAdmin();
                    dto.setId(client.getId());
                    dto.setName(client.getName());
                    dto.setEmail(client.getEmail());
                    dto.setPhone(client.getPhone());
                    return dto;
                })
                .collect(Collectors.toList());
    }


    // Versión para listados: la categoría va sin descripción
    private ProductSummaryDTO toSummaryDTO(Product product) {
        CategoryRefDTO category = null;

        if (product.getCategory() != null) {
            category = new CategoryRefDTO();
            category.setId(product.getCategory().getId());
            category.setName(product.getCategory().getName());
        }

        return new ProductSummaryDTO(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                category,
                product.getImageUrl(),
                product.getActive(),
                product.getPopular(),
                product.isVegetarian(),
                product.isSpicyMild(),
                product.isSpicyHot(),
                product.isContainsNuts(),
                product.isContainsSeafood(),
                product.isContainsGluten());
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
    public Collection<ProductSummaryDTO> getAllProducts() {
        return productService.getAllProducts()
                .stream().map(this::toSummaryDTO).toList();
    }

    @Operation(summary = "Get only id and name of all products (filter dropdowns)")
    @GetMapping("/options")
    public List<ProductOptionDTO> getProductOptions() {
        return productService.getProductOptions();
    }

    @Operation(summary = "Get all active products")
    @GetMapping("/active")
    public Collection<ProductSummaryDTO> getActiveProducts() {
        return productService.getAllProductsActive()
                .stream().map(this::toSummaryDTO).toList();
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable Integer id) {
        return toDTO(productService.getActiveProductById(id));
    }

    @Operation(summary = "Get popular products")
    @GetMapping("/popular")
    public Collection<ProductSummaryDTO> getPopularProducts() {
        return productService.getPopularProductsActive()
                .stream().map(this::toSummaryDTO).toList();
    }

    @Operation(summary = "Get up to 3 active products of the same category")
    @GetMapping("/{id}/related")
    public Collection<ProductSummaryDTO> getRelatedProducts(@PathVariable Integer id) {
        return productService.getRelatedProducts(id)
                .stream().map(this::toSummaryDTO).toList();
    }

    @Operation(summary = "Get only the ids of active products")
    @GetMapping("/active/ids")
    public List<Integer> getActiveProductIds() {
        return productService.getActiveProductIds();
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