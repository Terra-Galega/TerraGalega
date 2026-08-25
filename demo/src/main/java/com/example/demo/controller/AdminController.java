package com.example.demo.controller;

import com.example.demo.entities.AddOn;
import com.example.demo.entities.Product;
import com.example.demo.service.ProductService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.List;

/*
 * Controlador del panel de administración (/admin).
 * El acceso a estas páginas se protege desde el navegador (script.js) comprobando
 * en localStorage que la persona haya iniciado sesión con el rol "admin"; este
 * proyecto no usa Spring Security, así que no hay un guardado de sesión en el servidor.
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    // Categorías disponibles para el formulario de productos (mismo listado que en el front original)
    private static final List<String> CATEGORIES = List.of("Entradas", "Mariscos", "Carnes", "Postres");

    // http://localhost:8090/admin -> panel con la pestaña "Productos" (tabla)
    @GetMapping
    public String admin(Model model) {
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", CATEGORIES);
        return "admin";
    }

    // Construye la lista de adicionales a partir de los arreglos paralelos enviados por el formulario
    private List<AddOn> buildAdditionals(String[] additionalNames, String[] additionalPrices) {
        List<AddOn> additionals = new ArrayList<>();
        if (additionalNames == null) {
            return additionals;
        }
        for (int i = 0; i < additionalNames.length; i++) {
            String name = additionalNames[i];
            String priceStr = (additionalPrices != null && i < additionalPrices.length) ? additionalPrices[i] : null;
            // Ignora filas vacías (el usuario no llenó ese adicional)
            if (name == null || name.isBlank() || priceStr == null || priceStr.isBlank()) {
                continue;
            }
            additionals.add(new AddOn(i + 1, name.trim(), "Adicional", Double.valueOf(priceStr), true));
        }
        return additionals;
    }

    // Añade un producto nuevo desde el formulario del modal "Añadir producto"
    @PostMapping("/products")
    public String addProduct(
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam String imageUrl,
            @RequestParam(required = false) Boolean popular,
            @RequestParam(required = false) String[] additionalNames,
            @RequestParam(required = false) String[] additionalPrices) {
        Product product = new Product(
                null,
                name,
                description,
                price,
                category,
                imageUrl,
                buildAdditionals(additionalNames, additionalPrices),
                true,
                popular != null && popular);
                productService.addProduct(product);
        return "redirect:/admin";
    }

    // Guarda los cambios de un producto existente desde el modal "Editar producto"
    @PostMapping("/products/{id}/update")
    public String updateProduct(
            @PathVariable Integer id,
            @RequestParam String name,
            @RequestParam(required = false) String description,
            @RequestParam Double price,
            @RequestParam String category,
            @RequestParam String imageUrl,
            @RequestParam(required = false) Boolean popular,
            @RequestParam(required = false) String[] additionalNames,
            @RequestParam(required = false) String[] additionalPrices) {
        Product product = new Product(id,name,description,price,category,imageUrl,buildAdditionals(additionalNames, additionalPrices),
                true,
                popular != null && popular);
        productService.updateProduct(id, product);
        return "redirect:/admin";
    }

    // Elimina un producto de la carta
    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Integer id) {
        productService.deleteProduct(id);
        return "redirect:/admin";
    }

    // Activa/desactiva un producto (equivalente al toggle "Activo/Inactivo" del front original)
    @PostMapping("/products/{id}/toggle")
    public String toggleProduct(@PathVariable Integer id) {
        productService.toggleProductActive(id);
        return "redirect:/admin";
    }
}
