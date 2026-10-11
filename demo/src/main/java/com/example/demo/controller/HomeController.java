package com.example.demo.controller;

import com.example.demo.entities.Product;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import io.swagger.v3.oas.annotations.Operation;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Operation(summary = "Get the home page")
    @GetMapping({ "/", "/home" })
    public String home(Model model) {
        // Cargar todos los productos
        model.addAttribute("products", productService.getAllProductsActive());
        // Cargar solo los productos populares para la sección "Favoritos de la casa"
        model.addAttribute("popularProducts", productService.getPopularProductsActive());
        return "home";
    }

    @Operation(summary = "Get the menu page")
    @GetMapping("/menu")
    public String menu(Model model) {
        // Carga todos los productos para la carta completa y para el modal de producto
        model.addAttribute("products", productService.getAllProductsActive());
        return "menu";
    }

    @Operation(summary = "Get the about us page")
    @GetMapping("/aboutUs")
    public String aboutUs() {
        return "aboutUs";
    }

    @Operation(summary = "Get the contact page")
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    @Operation(summary = "Get the product detail page")
    @GetMapping("/productDetail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {

        // Busca el producto solicitado y si no existe vuelve a la carta
        Product product;

        product = productService.getProductById(id);

        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id));
        // Categoría completa
        model.addAttribute("addons", categoryService.getAddonsByCategory(product.getCategory().getName()));
        model.addAttribute("category", categoryService.getCategoryByName(product.getCategory().getName()));
        return "productDetail";
    }

}