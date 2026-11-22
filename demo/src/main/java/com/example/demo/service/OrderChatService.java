package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.Comparator;
import java.util.Objects;

import com.example.demo.dto.OrderChatDTOs.ChatSummaryDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.OrderChatDTOs.ChatDTO;
import com.example.demo.dto.OrderChatDTOs.MessageDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Operator;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderMessage;
import com.example.demo.entities.OrderStatus;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderMessageRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.security.AuthenticatedUser;

// Chat entre el cliente dueño del pedido y los operadores.
// Abierto desde que el pedido está pagado hasta que se entrega o se cancela.
@Service
public class OrderChatService {

    private static final Set<OrderStatus> CLOSED = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    private static final int MAX_LENGTH = 500;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMessageRepository messageRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private AuthenticatedUser authenticatedUser;

    @Autowired
    private NotificationService notificationService;

    private record Sender(String role, String name) {
    }

    @Transactional(readOnly = true)
    public ChatDTO getMessages(Integer orderId, Integer afterId) {
        Order order = loadOrder(orderId);
        resolveSender(order);

        List<OrderMessage> messages = afterId == null
                ? messageRepository.findByOrderIdOrderByIdAsc(orderId)
                : messageRepository.findByOrderIdAndIdGreaterThanOrderByIdAsc(orderId, afterId);

        return new ChatDTO(
                orderId,
                order.getStatus().name(),
                isOpen(order),
                messages.stream().map(this::toDTO).toList());
    }

    @Transactional
    public MessageDTO send(Integer orderId, String content) {
        Order order = loadOrder(orderId);
        Sender sender = resolveSender(order);

        if (!isOpen(order)) {
            throw ApiException.conflict("El chat de este pedido ya está cerrado.");
        }

        String text = content == null ? "" : content.trim();
        if (text.isEmpty()) {
            throw ApiException.badRequest("El mensaje no puede estar vacío.");
        }
        if (text.length() > MAX_LENGTH) {
            throw ApiException.badRequest("El mensaje no puede superar los " + MAX_LENGTH + " caracteres.");
        }

        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setSenderRole(sender.role());
        message.setSenderName(sender.name());
        message.setContent(text);
        message.setCreatedAt(LocalDateTime.now());

        OrderMessage saved = messageRepository.save(message);

        // Campanita del cliente: solo cuando escribe el operador
        if ("OPERATOR".equals(sender.role())) {
            notificationService.notifyChatMessage(order, text);
        }

        return toDTO(saved);        
    }

    // Chats activos: pedidos pagados, sin terminar y con al menos un mensaje.
    // Cliente: solo los suyos. Operador: todos.
    @Transactional(readOnly = true)
    public List<ChatSummaryDTO> getActiveChats() {
        String email = authenticatedUser.email();
        List<Order> orders;

        Client client = clientRepository.findByEmail(email);
        if (client != null) {
            orders = orderRepository.findByClientIdOrderByCreatedAtDesc(client.getId());
        } else {
            Operator operator = operatorRepository.findByEmail(email);
            if (operator == null || !Boolean.TRUE.equals(operator.getActive())) {
                throw ApiException.forbidden("Solo el cliente y los operadores pueden usar este chat.");
            }
            orders = orderRepository.findByStatusNotInOrderByCreatedAtAsc(CLOSED);
        }

        return orders.stream()
                .filter(order -> isOpen(order) && order.isProcessable())
                .map(order -> messageRepository.findTopByOrderIdOrderByIdDesc(order.getId())
                        .map(last -> new ChatSummaryDTO(
                                order.getId(),
                                order.getStatus().name(),
                                last.getId(),
                                last.getSenderRole(),
                                last.getCreatedAt()))
                        .orElse(null))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ChatSummaryDTO::lastMessageId).reversed())
                .toList();
    }
    // El pedido debe existir y estar pagado (los pendientes de pago no tienen chat)
    private Order loadOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApiException.notFound("El pedido no existe."));

        if (!order.isProcessable()) {
            throw ApiException.conflict("El chat se habilita cuando el pedido está pagado.");
        }
        return order;
    }

    private boolean isOpen(Order order) {
        return !CLOSED.contains(order.getStatus());
    }

    // Cliente: solo su propio pedido. Operador: cualquier pedido si está activo.
    private Sender resolveSender(Order order) {
        String email = authenticatedUser.email();

        Client client = clientRepository.findByEmail(email);
        if (client != null) {
            if (order.getClient() == null || !order.getClient().getId().equals(client.getId())) {
                throw ApiException.forbidden("Este pedido no es tuyo.");
            }
            return new Sender("CLIENT", fullName(client.getName(), client.getLastName()));
        }

        Operator operator = operatorRepository.findByEmail(email);
        if (operator != null) {
            if (!Boolean.TRUE.equals(operator.getActive())) {
                throw ApiException.forbidden("El operador está desactivado.");
            }
            return new Sender("OPERATOR", fullName(operator.getName(), operator.getLastName()));
        }

        throw ApiException.forbidden("Solo el cliente y los operadores pueden usar este chat.");
    }

    private String fullName(String name, String lastName) {
        String full = ((name == null ? "" : name) + " " + (lastName == null ? "" : lastName)).trim();
        return full.length() > 100 ? full.substring(0, 100) : full;
    }

    private MessageDTO toDTO(OrderMessage m) {
        return new MessageDTO(m.getId(), m.getSenderRole(), m.getSenderName(), m.getContent(), m.getCreatedAt());
    }
}