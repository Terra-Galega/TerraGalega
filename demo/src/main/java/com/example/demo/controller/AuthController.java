package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import com.example.demo.entities.Client;
import com.example.demo.entities.UserRole;
import com.example.demo.entities.Role;
import com.example.demo.service.CategoryService;
import com.example.demo.service.ClientService;
import com.example.demo.service.ProductService;
import com.example.demo.service.UserRoleService;
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
    private UserRoleService userRoleService;

    public static final String SESSION_Client = "ClientLogueado";

    private boolean isAdmin(Integer id) {
         UserRole userRole = userRoleService.getUserRoleById(id);
        return userRole != null && userRole.getRole() == Role.ADMIN;
    }

    // http://localhost:8080/admin
    @GetMapping("/admin/{id}")
    public String admin(@PathVariable Integer id, Model model, HttpSession session) {

        if (!isAdmin(id)) {

            // cliente que intenta entrar a /admin/<su-propio-id> (su id
            // es válido, pero no tiene rol admin) queda deslogueado,
            // igual que cuando alguien toca el id de otra persona
            session.invalidate();
            return "redirect:/login";
        }

        UserRole admin = userRoleService.getUserRoleById(id);
        model.addAttribute("products", productService.getAllProducts());
        model.addAttribute("categories", categoryService.getAllCategorys());
        model.addAttribute("adminName", admin.getName());
        model.addAttribute("adminId", admin.getId());
        return "admin";
    }

    // http://localhost:8080/login
    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        UserRole userRole = (UserRole) session.getAttribute(SESSION_Client);

        if (userRole != null) {

            if (isAdmin(userRole.getId())) {
                return "redirect:/admin/" + userRole.getId();
            } else {
                return "redirect:/account/" + userRole.getId();
            }
        }
        return "login";
    }

    @PostMapping("/login")
    public String doLogin(@RequestParam String email, @RequestParam String password,
            Model model, HttpSession session) {
        UserRole userRole = authService.login(email, password);

        if (userRole == null) {
            model.addAttribute("loginError", "Correo o contraseña incorrectos.");
            model.addAttribute("emailIngresado", email);
            return "login";
        }
        session.setAttribute(SESSION_Client, userRole);
        if (isAdmin(userRole.getId())) {
            return "redirect:/admin/" + userRole.getId();
        } else {
            return "redirect:/account/" + userRole.getId();
        }
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
    public String register(
        @RequestParam String name,
        @RequestParam String lastName,
        @RequestParam String email,
        @RequestParam String password,
        @RequestParam String phone,
        Model model,
        HttpSession session) {
        
        if (clientService.getAllClients().stream()
                .anyMatch(c -> c.getUserRole().getEmail().equalsIgnoreCase(email))) {
            model.addAttribute("signupError", "Ya existe una cuenta con ese correo.");
            return "error";
        }

        UserRole userRole = UserRole.builder()
                .name(name)
                .lastName(lastName)
                .email(email)
                .password(password)
                .role(Role.CLIENT)
                .build();

        Client client = Client.builder()
                .userRole(userRole)
                .phone(phone)
                .build();
        
        Client created = clientService.addClient(client);
        session.setAttribute(SESSION_Client, created.getUserRole());
        return "redirect:/account/" + created.getId();
    }

    // http://localhost:8080/account
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
