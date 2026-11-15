package com.example.demo.chat;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.example.demo.security.AuthenticatedUser;

import io.swagger.v3.oas.annotations.Operation;

// Chat del cliente con sesión iniciada. /clients/me/** ya exige el rol CLIENT en SecurityConfig.
@RestController
@RequestMapping("/clients/me/chat")
@CrossOrigin(origins = "http://localhost:4200")
public class ChatController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Operation(summary = "Send a message to the TerraGalega assistant (can add to cart and create reservations)")
    @PostMapping
    public ChatResponseDTO chat(@RequestBody ChatRequestDTO request) {
        return chatService.chat(authenticatedUser.client().getId(), request);
    }
}