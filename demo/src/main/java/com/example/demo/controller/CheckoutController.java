package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.movil.service.CartService;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/checkout")
@CrossOrigin(origins = "http://localhost:4200")
public class CheckoutController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CartService cartService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    // POST /checkout con el carrito en el cuerpo. El cliente se obtiene del token.
    @Operation(summary = "Process checkout for the logged-in client")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void checkout(@RequestBody CheckoutRequestDTO request) {
        Client client = authenticatedUser.client();

        orderService.checkout(client, request);

        // Se vacía el carrito compartido (web y app). Si falla, el pedido ya quedó creado.
        try {
            cartService.clear(client.getId());
        } catch (Exception ignored) {
        }
    }
}
