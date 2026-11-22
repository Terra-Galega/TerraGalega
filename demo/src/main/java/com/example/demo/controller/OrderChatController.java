package com.example.demo.controller;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.OrderChatDTOs.ChatDTO;
import com.example.demo.dto.OrderChatDTOs.ChatSummaryDTO;
import com.example.demo.dto.OrderChatDTOs.MessageDTO;
import com.example.demo.dto.OrderChatDTOs.SendMessageRequest;
import com.example.demo.entities.OrderAttachment;
import com.example.demo.service.OrderChatService;

import io.swagger.v3.oas.annotations.Operation;

// Chat cliente-operador de un pedido. Ruta: /order-chat
@RestController
@RequestMapping("/order-chat")
public class OrderChatController {

    @Autowired
    private OrderChatService orderChatService;

    // GET /order-chat/active
    @Operation(summary = "Get the active order chats of the logged-in client or operator")
    @GetMapping("/active")
    public List<ChatSummaryDTO> getActiveChats() {
        return orderChatService.getActiveChats();
    }

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

    // POST /order-chat/{orderId}/attachments  (multipart: file + content opcional)
    @Operation(summary = "Send a photo (JPG or PNG) in the chat of an order. Clients only")
    @PostMapping(value = "/{orderId}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MessageDTO sendPhoto(@PathVariable Integer orderId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "content", required = false) String content) {
        return orderChatService.sendPhoto(orderId, file, content);
    }

    // GET /order-chat/{orderId}/attachments/{attachmentId}
    @Operation(summary = "Get a photo of the chat of an order")
    @GetMapping("/{orderId}/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable Integer orderId,
            @PathVariable Integer attachmentId) {
        OrderAttachment attachment = orderChatService.getAttachment(orderId, attachmentId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(attachment.getContentType()))
                // Privada: solo el navegador de quien la pidió la guarda un rato
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS).cachePrivate())
                .header("X-Content-Type-Options", "nosniff")
                .body(attachment.getData());
    }
}