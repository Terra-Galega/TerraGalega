package com.example.demo.controller;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.SubscriptionDTO;
import com.example.demo.security.AuthenticatedUser;
import com.example.demo.service.SubscriptionService;

import io.swagger.v3.oas.annotations.Operation;

// El cliente se obtiene del token, nunca de un id en la URL.
// /subscriptions/** ya exige el rol CLIENT en SecurityConfig.
@RestController
@RequestMapping("/subscriptions")
@CrossOrigin(origins = "http://localhost:4200")
public class SubscriptionController {

    private final SubscriptionService subscriptions;
    private final AuthenticatedUser auth;

    public SubscriptionController(SubscriptionService subscriptions, AuthenticatedUser auth) {
        this.subscriptions = subscriptions;
        this.auth = auth;
    }

    @Operation(summary = "Get the subscription status of the logged-in client")
    @GetMapping("/status")
    public SubscriptionDTO status() {
        return subscriptions.status(auth.client().getId());
    }

    @Operation(summary = "Start (or renew) the Premium subscription payment")
    @PostMapping("/start")
    public SubscriptionDTO start() {
        return subscriptions.start(auth.client().getId());
    }
}