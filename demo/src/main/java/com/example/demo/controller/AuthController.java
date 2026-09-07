package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.demo.entities.Client;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ClientService;
import com.example.demo.service.ProductService;
import com.example.demo.service.AuthService;
import com.example.demo.service.UserService;
import com.example.demo.entities.User;
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
    private UserService userService;

    public static final String SESSION_Client = "ClientLogueado";

    private boolean isAdmin(Integer id) {
        User user = userService.getUserById(id);
        return user != null && user.isAdministrador();
    }

    // http://localhost:8090/admin
    @GetMapping("/admin/{id}")
    public String admin(@PathVariable Integer id, Model model, HttpSession session) {

        if (!isAdmin(id)) {

            // cliente que intenta entrar a /admin/<su-propio-id> (su id
            // es válido, pero no tiene rol admin) queda deslogueado,
            // igual que cuando alguien toca el id de otra persona
            session.invalidate();
            return "redirect:/login";
        }

        Client Client = clientService.getClientById(id);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategorys());
        model.addAttribute("adminName", Client.getName());
        model.addAttribute("adminId", Client.getId());
        return "admin";
    }

    // http://localhost:8090/login
    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        User user = (User) session.getAttribute(SESSION_Client);

        if (user != null) {

            if (isAdmin(user.getId())) {
                return "redirect:/admin/" + user.getId();
            } else {
                return "redirect:/account/" + user.getId();
            }
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
            Model model, HttpSession session) {
        User user = authService.login(email, password);

        if (user == null) {
            model.addAttribute("loginError", "Correo o contraseña incorrectos.");
            model.addAttribute("emailIngresado", email);
            return "login";
        }
        session.setAttribute(SESSION_Client, user);
        if (isAdmin(user.getId())) {
            return "redirect:/admin/" + user.getId();
        } else {
            return "redirect:/account/" + user.getId();
        }
    }

    // Procesa el formulario de registro
    // crea un Cliente real en ClientRepository y lo deja logueado

    // Cierra la sesión del Cliente
    @PostMapping("/logout")
    public String logout(HttpSession session) {
        session.removeAttribute(SESSION_Client);
        return "redirect:/home";
    }

    @PostMapping("/registro")
    public String registro(@ModelAttribute Client Client, Model model, HttpSession session) {
        if (clientService.getAllClients().stream()
                .anyMatch(c -> c.getEmail().equalsIgnoreCase(Client.getEmail()))) {
            model.addAttribute("signupError", "Ya existe una cuenta con ese correo.");
            return "login";
        }

        Client creado = clientService.addClient(Client);
        session.setAttribute(SESSION_Client, creado);
        return "redirect:/account/" + creado.getId();
    }

    // http://localhost:8090/account
    @GetMapping("/account/{id}")
    public String account(@PathVariable Integer id, Model model) {
        Client Client;

        try {
            Client = clientService.getClientById(id);
        } catch (Exception e) {
            return "redirect:/login";
        }

        model.addAttribute("client", Client);
        return "/account";
    }

}
