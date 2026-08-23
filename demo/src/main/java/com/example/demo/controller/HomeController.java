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
        // Cargar los productos desde el servicio a la vista
        model.addAttribute("products", productService.getAllProducts());
        return "home"; 
    }
}