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

    // Servicios necesarios para gestionar productos, categorías y consultar clientes.
    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ClientService clientService;

    // Convierte una entidad Product en un DTO con la información completa del producto.
    private ProductDTO toDTO(Product product) {
        ProductDTO dto = new ProductDTO();

        // Copia los datos principales y las características del producto.
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

        // Incluye los datos de la categoría cuando el producto tiene una asociada.
        if (product.getCategory() != null) {
            CategoryDTO categoryDTO = new CategoryDTO();
            categoryDTO.setId(product.getCategory().getId());
            categoryDTO.setName(product.getCategory().getName());
            categoryDTO.setDescription(product.getCategory().getDescription());
            dto.setCategory(categoryDTO);
        }

        return dto;
    }
    
    // Endpoint que devuelve los datos básicos de los clientes para el panel administrativo.
    @Operation(summary = "Get clients but only atributes: id, name, email, phone")
    @GetMapping("/clients")
    public Collection<ClientDTOAdmin> getClientDTOAdmins() {
        return clientService.getAllClients()
                .stream()
                .map(client -> {
                    // Construye un DTO con el identificador, nombre, correo y teléfono del cliente.
                    ClientDTOAdmin dto = new ClientDTOAdmin();
                    dto.setId(client.getId());
                    dto.setName(client.getName());
                    dto.setEmail(client.getEmail());
                    dto.setPhone(client.getPhone());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // Convierte un producto en un DTO resumido para mostrarlo en listados.
    // La categoría incluye únicamente su identificador y nombre.
    private ProductSummaryDTO toSummaryDTO(Product product) {
        CategoryRefDTO category = null;

        // Crea una referencia a la categoría si el producto tiene una asociada.
        if (product.getCategory() != null) {
            category = new CategoryRefDTO();
            category.setId(product.getCategory().getId());
            category.setName(product.getCategory().getName());
        }

        // Construye el resumen con los datos y características del producto.
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

    // Convierte los datos recibidos en un ProductDTO a una entidad Product.
    private Product toEntity(ProductDTO dto) {
        Product product = new Product();

        // Copia los datos principales y las características recibidas.
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

        // Busca la categoría por su ID y la asocia al producto cuando se proporciona.
        if (dto.getCategory() != null
                && dto.getCategory().getId() != null) {
            Category category = categoryService
                    .getCategoryById(dto.getCategory().getId());

            product.setCategory(category);
        }

        return product;
    }

    // Endpoint para obtener todos los productos en formato resumido.
    @Operation(summary = "Get all products")
    @GetMapping
    public Collection<ProductSummaryDTO> getAllProducts() {
        return productService.getAllProducts()
                .stream().map(this::toSummaryDTO).toList();
    }

    // Endpoint para obtener únicamente el ID y el nombre de los productos.
    @Operation(summary = "Get only id and name of all products (filter dropdowns)")
    @GetMapping("/options")
    public List<ProductOptionDTO> getProductOptions() {
        return productService.getProductOptions();
    }

    // Endpoint para consultar los productos activos.
    @Operation(summary = "Get all active products")
    @GetMapping("/active")
    public Collection<ProductSummaryDTO> getActiveProducts() {
        return productService.getAllProductsActive()
                .stream().map(this::toSummaryDTO).toList();
    }

    // Endpoint para consultar un producto por su ID y devolver sus datos completos.
    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ProductDTO getProductById(@PathVariable Integer id) {
        return toDTO(productService.getActiveProductById(id));
    }

    // Endpoint para obtener los productos populares que están activos.
    @Operation(summary = "Get popular products")
    @GetMapping("/popular")
    public Collection<ProductSummaryDTO> getPopularProducts() {
        return productService.getPopularProductsActive()
                .stream().map(this::toSummaryDTO).toList();
    }

    // Endpoint para obtener hasta tres productos relacionados con la categoría de otro producto.
    @Operation(summary = "Get up to 3 active products of the same category")
    @GetMapping("/{id}/related")
    public Collection<ProductSummaryDTO> getRelatedProducts(@PathVariable Integer id) {
        return productService.getRelatedProducts(id)
                .stream().map(this::toSummaryDTO).toList();
    }

    // Endpoint para obtener los identificadores de los productos activos.
    @Operation(summary = "Get only the ids of active products")
    @GetMapping("/active/ids")
    public List<Integer> getActiveProductIds() {
        return productService.getActiveProductIds();
    }

    // Endpoint para registrar un producto a partir de los datos recibidos.
    @Operation(summary = "Add a new product")
    @PostMapping
    public ProductDTO addProduct(@RequestBody ProductDTO dto) {
        return toDTO(productService.addProduct(toEntity(dto)));
    }

    // Endpoint para actualizar un producto existente mediante su ID.
    @Operation(summary = "Update a product")
    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable Integer id,
            @RequestBody ProductDTO dto) {
        return toDTO(productService.updateProduct(id, toEntity(dto)));
    }

    // Endpoint para eliminar un producto mediante su ID.
    @Operation(summary = "Delete a product")
    @DeleteMapping("/{id}")
    public void deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
    }

    // Endpoint para cambiar el estado de activación de un producto.
    @Operation(summary = "Toggle product active status")
    @PutMapping("/{id}/toggle")
    public ProductDTO toggleProduct(@PathVariable Integer id) {
        return toDTO(productService.toggleProductActive(id));
    }
}