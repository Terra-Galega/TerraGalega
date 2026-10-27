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

/**
 * Controlador de las páginas del sitio hecho con plantillas Thymeleaf (el front
 * anterior, antes de migrar a Angular).
 *
 * A diferencia de los demás controladores, este NO es REST: usa @Controller y
 * cada método devuelve el nombre de una plantilla HTML. Los datos se le pasan a
 * la vista a través del Model.
 */
@Controller
public class HomeController {

    // Servicio de productos: de aquí salen los platos que se muestran en las vistas
    @Autowired
    private ProductService productService;

    // Servicio de categorías: se usa en el detalle para traer los adicionales
    @Autowired
    private CategoryService categoryService;

    // GET / y GET /home -> página de inicio
    @Operation(summary = "Get the home page")
    @GetMapping({ "/", "/home" })
    public String home(Model model) {
        // Cargar todos los productos
        model.addAttribute("products", productService.getAllProductsActive());
        // Cargar solo los productos populares para la sección "Favoritos de la casa"
        model.addAttribute("popularProducts", productService.getPopularProductsActive());
        return "home";
    }

    // GET /menu -> carta completa
    @Operation(summary = "Get the menu page")
    @GetMapping("/menu")
    public String menu(Model model) {
        // Carga todos los productos para la carta completa y para el modal de producto
        model.addAttribute("products", productService.getAllProductsActive());
        return "menu";
    }

    // GET /aboutUs -> página "Sobre nosotros" (no necesita datos)
    @Operation(summary = "Get the about us page")
    @GetMapping("/aboutUs")
    public String aboutUs() {
        return "aboutUs";
    }

    // GET /contact -> página de contacto (no necesita datos)
    @Operation(summary = "Get the contact page")
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }

    // GET /productDetail/{id} -> detalle de un producto, con productos
    // relacionados y los adicionales de su categoría
    @Operation(summary = "Get the product detail page")
    @GetMapping("/productDetail/{id}")
    public String productDetail(@PathVariable Integer id, Model model) {

        // Busca el producto solicitado; si no existe, el servicio lanza
        // ProductNotFoundException
        Product product;

        product = productService.getProductById(id);

        model.addAttribute("product", product);
        // Otros productos activos de la misma categoría
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id));
        // Adicionales disponibles para la categoría del producto
        model.addAttribute("addons", categoryService.getAddonsByCategory(product.getCategory().getName()));
        // Categoría completa
        model.addAttribute("category", categoryService.getCategoryByName(product.getCategory().getName()));
        return "productDetail";
    }

}