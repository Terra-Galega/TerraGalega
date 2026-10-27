package com.example.demo.controller;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.service.ClientService;
import com.example.demo.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/checkout")
@CrossOrigin(origins = "http://localhost:4200")
public class CheckoutController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ClientService clientService;

    // POST /checkout?clientId=X con el carrito en el cuerpo 
    @Operation(summary = "Process checkout and create an order")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201 sin cuerpo: el pedido se creó
    public void checkout(
            @RequestParam Integer clientId,
            @RequestBody CheckoutRequestDTO request) {

        // Busca al cliente (lanza 404 si no existe)
        Client client = clientService.getClientById(clientId);

        // El pedido se guarda
        orderService.checkout(client, request);
    }
}