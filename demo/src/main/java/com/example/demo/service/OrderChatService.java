package com.example.demo.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.example.demo.dto.OrderChatDTOs.ChatDTO;
import com.example.demo.dto.OrderChatDTOs.ChatSummaryDTO;
import com.example.demo.dto.OrderChatDTOs.MessageDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Operator;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderAttachment;
import com.example.demo.entities.OrderMessage;
import com.example.demo.entities.OrderStatus;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderAttachmentRepository;
import com.example.demo.repository.OrderMessageRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.security.AuthenticatedUser;

// Chat entre el cliente dueño del pedido y los operadores.
// Abierto desde que el pedido está pagado hasta que se entrega o se cancela.
// El cliente también puede enviar fotos (JPG o PNG; las HEIC se convierten en el front).
@Service
public class OrderChatService {

    private static final Set<OrderStatus> CLOSED = EnumSet.of(OrderStatus.DELIVERED, OrderStatus.CANCELLED);
    private static final int MAX_LENGTH = 500;
    private static final long MAX_PHOTO_BYTES = 5L * 1024 * 1024;
    private static final int MAX_PHOTOS_PER_ORDER = 10;

    private static final byte[] PNG_SIGNATURE = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A };

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderMessageRepository messageRepository;

    @Autowired
    private OrderAttachmentRepository attachmentRepository;

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

    // Mensaje de solo texto (cliente u operador)
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
        checkLength(text);

        OrderMessage saved = messageRepository.save(newMessage(orderId, sender, text));

        // Campanita del cliente: solo cuando escribe el operador
        if ("OPERATOR".equals(sender.role())) {
            notificationService.notifyChatMessage(order, text);
        }

        return toDTO(saved);
    }

    // Foto del cliente, con comentario opcional
    @Transactional
    public MessageDTO sendPhoto(Integer orderId, MultipartFile file, String caption) {
        Order order = loadOrder(orderId);
        Sender sender = resolveSender(order);

        if (!"CLIENT".equals(sender.role())) {
            throw ApiException.forbidden("Solo el cliente puede enviar fotos.");
        }
        if (!isOpen(order)) {
            throw ApiException.conflict("El chat de este pedido ya está cerrado.");
        }
        if (file == null || file.isEmpty()) {
            throw ApiException.badRequest("Falta la foto.");
        }
        if (file.getSize() > MAX_PHOTO_BYTES) {
            throw ApiException.badRequest("La foto no puede pesar más de 5 MB.");
        }
        if (attachmentRepository.countByOrderId(orderId) >= MAX_PHOTOS_PER_ORDER) {
            throw ApiException.conflict(
                    "Ya enviaste el máximo de " + MAX_PHOTOS_PER_ORDER + " fotos para este pedido.");
        }

        byte[] data;
        try {
            data = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("No se pudo leer la foto.");
        }

        // Se decide por los bytes, no por el nombre ni por lo que diga el navegador
        String contentType = detectImageType(data);
        if (contentType == null) {
            throw ApiException.badRequest("Solo se aceptan fotos JPG o PNG.");
        }

        String text = caption == null ? "" : caption.trim();
        checkLength(text);

        OrderAttachment attachment = new OrderAttachment();
        attachment.setOrderId(orderId);
        attachment.setContentType(contentType);
        attachment.setSize(data.length);
        attachment.setData(data);
        attachment.setCreatedAt(LocalDateTime.now());
        attachment = attachmentRepository.save(attachment);

        OrderMessage message = newMessage(orderId, sender, text.isEmpty() ? null : text);
        message.setAttachmentId(attachment.getId());

        return toDTO(messageRepository.save(message));
    }

    // Foto de un pedido: solo para el cliente dueño y los operadores
    @Transactional(readOnly = true)
    public OrderAttachment getAttachment(Integer orderId, Integer attachmentId) {
        Order order = loadOrder(orderId);
        resolveSender(order);

        return attachmentRepository.findById(attachmentId)
                .filter(attachment -> attachment.getOrderId().equals(orderId))
                .orElseThrow(() -> ApiException.notFound("La foto no existe."));
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

    // ---------- Ayudantes ----------

    private OrderMessage newMessage(Integer orderId, Sender sender, String content) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(orderId);
        message.setSenderRole(sender.role());
        message.setSenderName(sender.name());
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private void checkLength(String text) {
        if (text.length() > MAX_LENGTH) {
            throw ApiException.badRequest("El mensaje no puede superar los " + MAX_LENGTH + " caracteres.");
        }
    }

    // JPG empieza por FF D8 FF; PNG por su firma de 8 bytes. Cualquier otra cosa se rechaza.
    private String detectImageType(byte[] data) {
        if (data.length >= 3
                && (data[0] & 0xFF) == 0xFF
                && (data[1] & 0xFF) == 0xD8
                && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data.length >= PNG_SIGNATURE.length
                && Arrays.equals(Arrays.copyOf(data, PNG_SIGNATURE.length), PNG_SIGNATURE)) {
            return "image/png";
        }
        return null;
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
        return new MessageDTO(
                m.getId(),
                m.getSenderRole(),
                m.getSenderName(),
                m.getContent(),
                m.getAttachmentId(),
                m.getCreatedAt());
    }
}