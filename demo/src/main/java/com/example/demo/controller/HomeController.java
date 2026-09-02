package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.entities.Product;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ClientService;
import com.example.demo.service.ProductService;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class HomeController {

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ClientService clientService;

    public static final String SESSION_Client = "ClientLogueado";

    // http://localhost:8090/
    // http://localhost:8090/home
    @GetMapping({ "/", "/home" })
    public String home(Model model) {
        // Cargar todos los productos
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
        // Busca el producto solicitado y si no existe vuelve a la carta
        Product product = productService.getProductById(id);
        if (product == null) {
            return "redirect:/menu";
        }
        model.addAttribute("product", product);
        model.addAttribute("relatedProducts", productService.getRelatedProducts(id));
        // Categoría completa
        model.addAttribute("addons", categoryService.getAddonsByCategory(product.getCategory()));
        model.addAttribute("category", categoryService.getCategoryByName(product.getCategory()));
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
    public String login(Model model, HttpSession session) {
        Client Client = (Client) session.getAttribute(SESSION_Client);
        if (Client != null) {
            return Boolean.TRUE.equals(Client.getAdmin()) ? "redirect:/admin" : "redirect:/account";
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
            Model model, HttpSession session) {
        Client Client = clientService.login(email, password);
        if (Client == null) {
            model.addAttribute("loginError", "Correo o contraseña incorrectos.");
            model.addAttribute("emailIngresado", email);
            return "login";
        }
        session.setAttribute(SESSION_Client, Client);
        if (Boolean.TRUE.equals(Client.getAdmin())) {
            return "redirect:/admin";
        }
        return "redirect:/account";
    }

    // Procesa el formulario de registro
    // crea un Cliente real en ClientRepository y lo deja logueado
    @PostMapping("/registro")
    public String registro(@ModelAttribute Client Client, Model model, HttpSession session) {
        if (clientService.getAllClients().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(Client.getEmail()))) {
            model.addAttribute("signupError", "Ya existe una account con ese correo.");
            return "login";
        }
        Client creado = clientService.addClient(Client);
        session.setAttribute(SESSION_Client, creado);
        return "redirect:/account";
    }

    // Cierra la sesión del Cliente
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_Client);
        return "redirect:/home";
    }

    // http://localhost:8090/account
    @GetMapping("/account")
    public String account(Model model, HttpSession session) {
        Client Client = (Client) session.getAttribute(SESSION_Client);
        if (Client == null) {
            return "redirect:/login";
        }
        model.addAttribute("client", Client);
        return "account";
    }

}