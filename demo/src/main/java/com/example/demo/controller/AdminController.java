package com.example.demo.controller;

import com.example.demo.entities.Admin;
import com.example.demo.entities.Product;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ProductService;
import com.example.demo.service.ClientService;
import com.example.demo.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AdminService adminService;

    @Autowired
    private ClientService clientService;

    @GetMapping("/{adminId}")
    public void getAllData(@PathVariable Integer id) {
        Admin admin = adminService.getAdminById(id);

        productService.getAllProducts();
        categoryService.getAllCategorys();
        admin.getName();
        admin.getId();
        clientService.getAllClients();

        // return admin;
    }

    // Añade un producto nuevo usando @ModelAttribute
    @PostMapping("/{adminId}/products")
    public void addProduct(@RequestBody Product product) {
        productService.addProduct(product);
    }

    // Guarda los cambios de un producto existente usando @ModelAttribute
    @PutMapping("/{adminId}/products/{productId}/update")
    public void updateProduct(@PathVariable Integer productId, @RequestBody Product product) {
        productService.updateProduct(productId, product);
    }

    // Elimina un producto de la carta
    @DeleteMapping("/{adminId}/products/{productId}/delete")
    public void deleteProduct(@PathVariable Integer adminId, @PathVariable Integer productId) {
        productService.deleteProduct(productId);
    }

    // Activa/desactiva un producto
    @PutMapping("/{adminId}/products/{productId}/toggle")
    public void toggleProduct(@PathVariable Integer productId) {
        productService.toggleProductActive(productId);
    }
}