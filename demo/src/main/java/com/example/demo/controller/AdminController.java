package com.example.demo.controller;

import com.example.demo.entities.AddOn;
import com.example.demo.entities.Client;
import com.example.demo.entities.Product;
import com.example.demo.service.ProductService;
import com.example.demo.entities.Category;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    // Categorías disponibles para el formulario de productos
    private static final List<String> CATEGORIES = List.of("Entradas", "Mariscos", "Carnes", "Postres");

    // Revisa que en la sesión haya un Client logueado con admin = true
    private boolean isAdmin(HttpSession session) {
        Object attr = session.getAttribute(HomeController.SESSION_Client);
        return attr instanceof Client && Boolean.TRUE.equals(((Client) attr).getAdmin());
    }

    // http://localhost:8090/admin
    @GetMapping
    public String admin(Model model, HttpSession session) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        Client Client = (Client) session.getAttribute(HomeController.SESSION_Client);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("adminName", Client.getName());
        return "admin";
    }

    // Añade un producto nuevo usando @ModelAttribute
    @PostMapping("/products")
    public String addProduct(
            HttpSession session,
            @ModelAttribute Product product) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // El objeto 'product' ya viene completamente poblado desde el formulario HTML
        productService.addProduct(product);
        return "redirect:/admin";
    }

    // Guarda los cambios de un producto existente usando @ModelAttribute
    @PostMapping("/products/{id}/update")
    public String updateProduct(
            HttpSession session,
            @PathVariable Integer id,
            @ModelAttribute Product product) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }

        // Aseguramos que el ID de la URL quede asignado al objeto
        product.setId(id);

        productService.updateProduct(id, product);
        return "redirect:/admin";
    }

    // Elimina un producto de la carta
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(HttpSession session, @PathVariable Integer id) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        productService.deleteProduct(id);
        return "redirect:/admin";
    }

    // Activa/desactiva un producto
    @PostMapping("/products/{id}/toggle")
    public String toggleProduct(HttpSession session, @PathVariable Integer id) {
        if (!isAdmin(session)) {
            return "redirect:/login";
        }
        productService.toggleProductActive(id);
        return "redirect:/admin";
    }
}
