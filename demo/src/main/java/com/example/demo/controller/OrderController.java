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

    // http://localhost:8080/orders
    // Requiere sesión de cliente, igual que /account. Muestra los pedidos
    // ya guardados (client, items con producto/cantidad/adicionales, total,
    // estado) más recientes primero
    @GetMapping("/orders")
    public String orders(Model model, HttpSession session) {
        Object logged = session.getAttribute(AuthController.SESSION_Client);

        if (!(logged instanceof Client client)) {
            return "redirect:/login";
        }

        model.addAttribute("client", client);
        model.addAttribute("orders", orderService.getOrdersByClientId(client.getId()));
        return "orders";
    }
}