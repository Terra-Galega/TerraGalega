package com.example.demo.controller;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.service.OrderService;
import com.example.demo.entities.Order;

import jakarta.servlet.http.HttpSession;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

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
public ResponseEntity<?> pay(
        @RequestBody CheckoutRequestDTO request,
        HttpSession session) {

    Object logged = session.getAttribute(AuthController.SESSION_Client);

    if (!(logged instanceof Client client)) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    try {
        Order order = orderService.checkout(client, request);

        Order savedOrder = orderService.getOrderById(order.getId());

        return ResponseEntity.ok(Map.of("orderId", savedOrder.getId()));

    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
    }
}
}