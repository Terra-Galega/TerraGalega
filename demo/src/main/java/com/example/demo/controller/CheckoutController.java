
package com.example.demo.controller;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Order;
import com.example.demo.service.ClientService;
import com.example.demo.service.OrderService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    @Operation(summary = "Process checkout and create an order")
    @PostMapping
    public Order checkout(
            @RequestParam Integer clientId,
            @RequestBody CheckoutRequestDTO request) {

        Client client = clientService.getClientById(clientId);

        return orderService.checkout(client, request);
    }
}
