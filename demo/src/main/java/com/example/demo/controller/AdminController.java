package com.example.demo.controller;

import com.example.demo.entities.Product;
import com.example.demo.service.ProductService;
import com.example.demo.entities.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import jakarta.servlet.http.HttpSession;


@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private ProductService productService;

    // Verificamos si el usuario logueado es administrador desde la sesión. Si no lo
    // es, se invalida la sesión y se redirige al login.
    private boolean isntAdmin(HttpSession session) {
        User loggedUser = (session != null)
                ? (User) session.getAttribute(AuthController.SESSION_Client)
                : null;

        return loggedUser == null || !loggedUser.isAdministrador();
    }

    // Añade un producto nuevo usando @ModelAttribute
    @PostMapping("/{id}/products")
    public String addProduct(
            @PathVariable Integer id,
            @ModelAttribute Product product, HttpSession session) {

        if (isntAdmin(session)) {
            session.invalidate();
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
            @ModelAttribute Product product, HttpSession session) {

        // Aseguramos que el ID de la URL quede asignado al objeto
        product.setId(productId);

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }

        productService.updateProduct(productId, product);
        return "redirect:/admin/" + id;
    }

    // Elimina un producto de la carta
    @PostMapping("/{id}/products/{productId}/delete")
    public String deleteProduct(
            @PathVariable Integer id,
            @PathVariable Integer productId, HttpSession session) {

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }

        productService.deleteProduct(productId);
        return "redirect:/admin/" + id;
    }

    // Activa/desactiva un producto
    @PostMapping("/{id}/products/{productId}/toggle")
    public String toggleProduct(
            @PathVariable Integer id,
            @PathVariable Integer productId, HttpSession session) {

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }
        productService.toggleProductActive(productId);
        return "redirect:/admin/" + id;
    }
}