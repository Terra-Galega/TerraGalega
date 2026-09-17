package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.demo.entities.Admin;
import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.service.AdminService;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ClientService;
import com.example.demo.service.ProductService;
import com.example.demo.service.AuthService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ClientService clientService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private AdminService adminService;

    // En sesión se guarda directamente el Client o el Admin que inició
    // sesión (no hay clase intermedia). Quien lo lee usa instanceof.
    public static final String SESSION_Client = "loggedInClient";

    // Client y Admin ya no comparten tabla ni clase: "es admin" se resuelve
    // preguntando directamente a la tabla admins por ese id.
    private boolean isAdmin(Integer id) {
        try {
            return adminService.getAdminById(id) != null;
        } catch (RuntimeException e) {
            return false;
        }
    }

    // http://localhost:8080/admin
    @GetMapping("/admin/{id}")
    public String admin(@PathVariable Integer id, Model model, HttpSession session) {

        if (!isAdmin(id)) {

            // cliente que intenta entrar a /admin/<su propio id> (su id
            // es válido, pero no tiene rol admin) queda deslogueado,
            // igual que cuando alguien toca el id de otra persona
            session.invalidate();
            return "redirect:/login";
        }

        Admin admin = adminService.getAdminById(id);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategorys());
        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminId", admin.getId());
        model.addAttribute("clients", clientService.getAllClients());
        return "admin";
    }

    // http://localhost:8080/login
    @GetMapping("/login")
    public String login(@RequestParam(required = false) String redirect, Model model, HttpSession session) {
        Object logged = session.getAttribute(SESSION_Client);

        if (logged instanceof Client client) {
            if ("checkout".equals(redirect)) {
                return "redirect:/checkout";
            }
            return "redirect:/account/" + client.getId();
        }

        if (logged instanceof Admin admin) {
            return "redirect:/admin/" + admin.getId();
        }

        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
            @RequestParam(required = false) String redirect,
            Model model, HttpSession session) {

        Object logged = authService.login(email, password);

        if (logged == null) {
            model.addAttribute("loginError", "Correo o contraseña incorrectos.");
            model.addAttribute("emailIngresado", email);
            return "login";
        }

        session.setAttribute(SESSION_Client, logged);

        if (logged instanceof Admin admin) {
            return "redirect:/admin/" + admin.getId();
        }

        if (logged instanceof Client client) {
            if ("checkout".equals(redirect)) {
                return "redirect:/checkout";
            }
            return "redirect:/account/" + client.getId();
        }

        return "redirect:/home";
    }

    // Cierra la sesión del Cliente
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_Client);
        return "redirect:/home";
    }

    // Procesa el formulario de registro
    // crea un Cliente real en ClientRepository y lo deja logueado
    @PostMapping("/register")
    public String register(@ModelAttribute Client client, @RequestParam(required = false) String redirect,
            Model model, HttpSession session) {

        boolean emailInUse = clientService.findByEmail(client.getEmail()) != null
                || adminService.findByEmail(client.getEmail()) != null;

        if (emailInUse) {
            model.addAttribute("signupError", "Ya existe una cuenta con ese correo.");
            return "login";
        }

        client.setRole(Role.CLIENT);

        Client created = clientService.addClient(client);
        session.setAttribute(SESSION_Client, created);

        if ("checkout".equals(redirect)) {
            return "redirect:/checkout";
        }
        return "redirect:/account/" + created.getId();
    }


    // http://localhost:8080/account
    @GetMapping("/account/{id}")
    public String account(@PathVariable Integer id, Model model) {
        Client client;

        try {
            client = clientService.getClientById(id);
        } catch (Exception e) {
            return "redirect:/login";
        }

        model.addAttribute("client", client);
        return "/account";
    }

}