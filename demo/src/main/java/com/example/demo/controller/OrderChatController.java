package com.example.demo.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;

import com.example.demo.dto.OrderChatDTOs.ChatSummaryDTO;
import com.example.demo.dto.OrderChatDTOs.ChatDTO;
import com.example.demo.dto.OrderChatDTOs.MessageDTO;
import com.example.demo.dto.OrderChatDTOs.SendMessageRequest;
import com.example.demo.service.OrderChatService;

import io.swagger.v3.oas.annotations.Operation;

// Chat cliente-operador de un pedido. Ruta: /order-chat
@RestController
@RequestMapping("/order-chat")
public class OrderChatController {

    @Autowired
    private OrderChatService orderChatService;

    // GET /order-chat/{orderId}/messages?after=ultimoId
    @Operation(summary = "Get the chat messages of an order (only newer than 'after' if sent)")
    @GetMapping("/{orderId}/messages")
    public ChatDTO getMessages(@PathVariable Integer orderId,
            @RequestParam(required = false) Integer after) {
        return orderChatService.getMessages(orderId, after);
    }

    // POST /order-chat/{orderId}/messages  { "content": "..." }
    @Operation(summary = "Send a chat message about an order")
    @PostMapping("/{orderId}/messages")
    public MessageDTO send(@PathVariable Integer orderId, @RequestBody SendMessageRequest request) {
        return orderChatService.send(orderId, request.content());
    }
    // GET /order-chat/active
    @Operation(summary = "Get the active order chats of the logged-in client or operator")
    @GetMapping("/active")
    public List<ChatSummaryDTO> getActiveChats() {
        return orderChatService.getActiveChats();
    }
}