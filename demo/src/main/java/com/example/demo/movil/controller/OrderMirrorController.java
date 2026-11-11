package com.example.demo.movil.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.movil.dto.CartDTOs.MirrorCheckDTO;
import com.example.demo.movil.service.OrderMirrorService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/sync")
public class OrderMirrorController {

    private static final Logger log = LoggerFactory.getLogger(OrderMirrorController.class);

    @Autowired
    private OrderMirrorService mirror;

    @Operation(summary = "Rebuild the Postgres copy of every order from H2")
    @PostMapping("/orders")
    public int resync() {
        return mirror.resyncAll();
    }

    @Operation(summary = "Compare H2 orders with their Postgres copy")
    @GetMapping("/orders/check")
    public MirrorCheckDTO check() {
        return mirror.check();
    }

    // Después de que los DataLoader cargan los pedidos de ejemplo, se copian a
    // Postgres
    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        try {
            log.info("Pedidos copiados a Postgres al arrancar: {}", mirror.resyncAll());
        } catch (Exception e) {
            log.warn("No se pudo copiar los pedidos a Postgres al arrancar: {}", e.getMessage());
        }
    }
}