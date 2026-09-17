package com.example.demo.controller;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.service.OrderService;
import com.example.demo.entities.Order;

import jakarta.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class CheckoutController {

    @Autowired
    private OrderService orderService;

    // Muestra la página de checkout.
    // Si el usuario no ha iniciado sesión,
    // lo manda al login y después puede volver al checkout.
    @GetMapping("/checkout")
    public String checkoutPage(Model model, HttpSession session) {

        Object logged = session.getAttribute(AuthController.SESSION_Client);

        if (!(logged instanceof Client client)) {
            return "redirect:/login?redirect=checkout";
        }

        model.addAttribute("client", client);

        return "checkout";
    }

    // Procesa el pago simulado.
    @PostMapping("/checkout")
    public String pay(
            @ModelAttribute CheckoutRequestDTO request,
            HttpSession session) {

        Object logged = session.getAttribute(AuthController.SESSION_Client);

        if (!(logged instanceof Client client)) {
            return "redirect:/login?redirect=checkout";
        }

        try {
            Order order = orderService.checkout(client, request);

            return "redirect:/orders" + order.getId();

        } catch (IllegalArgumentException e) {
            return "redirect:/checkout?error=" + e.getMessage();
        }
    }
}