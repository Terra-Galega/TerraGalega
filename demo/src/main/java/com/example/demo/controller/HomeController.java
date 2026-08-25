package com.example.demo.controller;

import com.example.demo.entities.Product;
import com.example.demo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    // http://localhost:8090/
    // http://localhost:8090/home
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Cargar todos los productos (se usan como catálogo en memoria para el modal de producto)
        model.addAttribute("products", productService.getAllProducts());
        // Cargar solo los productos populares para la sección "Favoritos de la casa"
        model.addAttribute("popularProducts", productService.getPopularProducts());
        return "home"; 
    }

    // http://localhost:8090/menu
    @GetMapping("/menu")
    public String menu(Model model) {
        // Carga todos los productos para la carta completa y para el modal de producto
        model.addAttribute("products", productService.getAllProducts());
        return "menu";
    }

    // http://localhost:8090/productDetail/{id}
    @GetMapping("/productDetail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {
        // Busca el producto solicitado; si no existe, vuelve a la carta
        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/menu";
        }
        model.addAttribute("product", product);
        // Hasta 3 productos de la misma categoría para "También te puede gustar"
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id));
        return "productDetail";
    }

    // http://localhost:8090/nosotros
    @GetMapping("/aboutUs")
    public String aboutUs() {
        return "aboutUs";
    }

    // http://localhost:8090/contacto
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    // http://localhost:8090/login
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}