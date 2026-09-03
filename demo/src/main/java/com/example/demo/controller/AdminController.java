package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.entities.Product;
import com.example.demo.service.ClientService;
import com.example.demo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminController {

@Autowired
private ProductService productService;

@Autowired
private ClientService clientService;

// Categorías disponibles para el formulario de productos
private static final List<String> CATEGORIES = List.of("Entradas", "Mariscos", "Carnes", "Postres");

// Revisa que en la sesión haya un Client logueado con admin = true
private boolean isAdmin(Integer id) {
    Client client = clientService.getClientById(id);
    return client != null && Boolean.TRUE.equals(client.getAdmin());
}

// http://localhost:8090/admin
@GetMapping("/{id}")
public String admin(@PathVariable Integer id, Model model) {
    if (!isAdmin(id)) {
        return "redirect:/login";
    }

    Client Client = clientService.getClientById(id);
    model.addAttribute("products", productService.getAllProducts());
    model.addAttribute("categories", CATEGORIES);
    model.addAttribute("adminName", Client.getName());
    model.addAttribute("adminId", Client.getId());
    return "admin";
}

// Añade un producto nuevo usando @ModelAttribute
@PostMapping("/{id}/products")
public String addProduct(
        @PathVariable Integer id,
        @ModelAttribute Product product) {

    if (!isAdmin(id)) {
        return "redirect:/login";
    }

    // El objeto 'product' ya viene completamente poblado desde el formulario HTML
    productService.addProduct(product);
    return "redirect:/admin/" + id;
}

// Guarda los cambios de un producto existente usando @ModelAttribute
@PostMapping("/{id}/products/{productId}/update")
public String updateProduct(
        @PathVariable Integer id,
        @PathVariable Integer productId,
        @ModelAttribute Product product) {

    if (!isAdmin(id)) {
        return "redirect:/login";
    }

    // Aseguramos que el ID de la URL quede asignado al objeto
    product.setId(productId);

    productService.updateProduct(productId, product);
    return "redirect:/admin/" + id;
}

// Elimina un producto de la carta
@PostMapping("/{id}/products/{productId}/delete")
public String deleteProduct(
        @PathVariable Integer id,
        @PathVariable Integer productId) {

    if (!isAdmin(id)) {
        return "redirect:/login";
    }

    productService.deleteProduct(productId);
    return "redirect:/admin/" + id;
}

// Activa/desactiva un producto
@PostMapping("/{id}/products/{productId}/toggle")
public String toggleProduct(
        @PathVariable Integer id,
        @PathVariable Integer productId) {

    if (!isAdmin(id)) {
        return "redirect:/login";
    }

    productService.toggleProductActive(productId);
    return "redirect:/admin/" + id;
}
}