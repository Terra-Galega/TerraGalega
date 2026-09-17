package com.example.demo.controller;

import com.example.demo.entities.Client;
import com.example.demo.service.OrderService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;

    @GetMapping("/orders")
    public String orders(Model model, HttpSession session) {
        Object logged = session.getAttribute(AuthController.SESSION_Client);

        // Si es un cliente válido, pasamos su información y sus pedidos
        if (logged instanceof Client client) {
            model.addAttribute("client", client);
            model.addAttribute("orders", orderService.getOrdersByClientId(client.getId()));
        } else {
            // Si no hay sesión, mandamos el client como nulo explícitamente
            // para que Thymeleaf muestre los pasos de iniciar sesión y pedir
            model.addAttribute("client", null);
        }

        // Siempre retornamos la misma vista, el HTML decidirá qué mostrar
        return "orders";
    }
}