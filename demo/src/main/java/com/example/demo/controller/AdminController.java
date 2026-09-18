package com.example.demo.controller;

import com.example.demo.entities.Product;
import com.example.demo.service.ProductService;
import com.example.demo.entities.Admin;

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
        Object logged = (session != null)
                ? session.getAttribute(AuthController.SESSION_Client)
                : null;

        return !(logged instanceof Admin);
    }

    // Añade un producto nuevo usando @ModelAttribute
    @PostMapping("/{adminId}/products")
    public String addProduct(
            @PathVariable Integer adminId,
            @ModelAttribute Product product, HttpSession session) {

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }

        // Por seguridad, nunca confiamos en un id que pudiera venir pegado
        // al producto (antes chocaba con el path variable del admin y
        // terminaba pisando el producto id=1 en vez de crear uno nuevo).
        product.setId(null);

        // El objeto 'product' ya viene completamente poblado desde el formulario HTML
        productService.addProduct(product);
        return "redirect:/admin/" + adminId;
    }

    // Guarda los cambios de un producto existente usando @ModelAttribute
    @PostMapping("/{adminId}/products/{productId}/update")
    public String updateProduct(
            @PathVariable Integer adminId,
            @PathVariable Integer productId,
            @ModelAttribute Product product, HttpSession session) {

        // Aseguramos que el ID de la URL quede asignado al objeto
        product.setId(productId);

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }

        productService.updateProduct(productId, product);
        return "redirect:/admin/" + adminId;
    }

    // Elimina un producto de la carta
    @PostMapping("/{adminId}/products/{productId}/delete")
    public String deleteProduct(
            @PathVariable Integer adminId,
            @PathVariable Integer productId, HttpSession session) {

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }

        productService.deleteProduct(productId);
        return "redirect:/admin/" + adminId;
    }

    // Activa/desactiva un producto
    @PostMapping("/{adminId}/products/{productId}/toggle")
    public String toggleProduct(
            @PathVariable Integer adminId,
            @PathVariable Integer productId, HttpSession session) {

        if (isntAdmin(session)) {
            session.invalidate();
            return "redirect:/login";
        }
        productService.toggleProductActive(productId);
        return "redirect:/admin/" + adminId;
    }
}