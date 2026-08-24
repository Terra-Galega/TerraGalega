package com.example.demo.controller;

import com.example.demo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    // http://localhost:8080/
    // http://localhost:8080/home
    @GetMapping({"/", "/home"})
    public String home(Model model) {
        // Cargar todos los productos (se usan como catálogo en memoria para el modal de producto)
        model.addAttribute("products", productService.getAllProducts());
        // Cargar solo los productos populares para la sección "Favoritos de la casa"
        model.addAttribute("popularProducts", productService.getPopularProducts());
        return "home"; 
    }
}