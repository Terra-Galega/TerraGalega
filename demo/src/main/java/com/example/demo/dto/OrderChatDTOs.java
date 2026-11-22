package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

// DTOs del chat cliente-operador de un pedido
public final class OrderChatDTOs {

    private OrderChatDTOs() {
    }

    // content puede ser null (foto sin comentario); attachmentId es null si no hay foto
    public record MessageDTO(
            Integer id,
            String senderRole,
            String senderName,
            String content,
            Integer attachmentId,
            LocalDateTime createdAt) {
    }

    public record SendMessageRequest(String content) {
    }

    // open = false cuando el pedido ya terminó (entregado o cancelado)
    public record ChatDTO(
            Integer orderId,
            String orderStatus,
            boolean open,
            List<MessageDTO> messages) {
    }
    // Pedido activo que ya tiene mensajes
    public record ChatSummaryDTO(
            Integer orderId,
            String orderStatus,
            Integer lastMessageId,
            String lastSenderRole,
            LocalDateTime lastMessageAt) {
    }
}