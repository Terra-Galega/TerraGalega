package com.example.demo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entities.Announcement;
import com.example.demo.entities.Client;
import com.example.demo.entities.Coupon;
import com.example.demo.entities.Notification;
import com.example.demo.entities.NotificationType;
import com.example.demo.entities.Order;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.NotificationRepository;

// Crea y lee las notificaciones de los clientes.
@Service
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Autowired
    private ClientRepository clientRepository;

    // Guarda una notificación nueva (sin leer) para un cliente
    private void create(Integer clientId, NotificationType type, String title, String message, Integer orderId) {
        Notification notification = new Notification();
        notification.setClientId(clientId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setOrderId(orderId);
        notification.setRead(false);
        notification.setCreatedAt(LocalDateTime.now());
        repository.save(notification);
    }

    // Avisos del pedido: confirmado, preparando, salió y ya va llegando
    @Transactional
    public void notifyOrder(Order order, NotificationType type) {
        // Si el cliente borró su cuenta, el pedido queda sin cliente
        if (order.getClient() == null) {
            return;
        }
        Integer clientId = order.getClient().getId();

        // Cada aviso de un pedido sale una sola vez
        if (repository.existsByClientIdAndTypeAndOrderId(clientId, type, order.getId())) {
            return;
        }

        String title;
        String message;
        switch (type) {
            case ORDER_CONFIRMED:
                title = "¡Tu pedido ya se confirmó!";
                message = "Pedido #" + order.getId() + ": el restaurante confirmó tu pedido.";
                break;
            case ORDER_PREPARING:
                title = "¡Estamos preparando tu pedido!";
                message = "Pedido #" + order.getId() + ": ya se está preparando en la cocina.";
                break;
            case ORDER_ON_THE_WAY:
                title = "¡Tu pedido ya salió!";
                message = "Pedido #" + order.getId() + ": tu repartidor inició el recorrido. Síguelo en el mapa.";
                break;
            case ORDER_NEAR:
                title = "¡Ya va llegando! Alístate";
                message = "Pedido #" + order.getId() + ": tu repartidor está a punto de llegar. Ten tu PIN a la mano.";
                break;
            default:
                return;
        }
        create(clientId, type, title, message, order.getId());
    }
    
    // Mensaje del operador en el chat del pedido.
    // Si el cliente aún no ha leído el aviso de ese pedido, se actualiza el mismo
    // (último mensaje y hora) en vez de llenar la campanita.
    @Transactional
    public void notifyChatMessage(Order order, String content) {
        if (order.getClient() == null) {
            return;
        }
        Integer clientId = order.getClient().getId();

        // El título debe ser igual al valor de "notifications.order_chat_message" en es.json
        // (el front lo traduce con el pipe t)
        String title = "Nuevo mensaje del restaurante";
        String preview = content.length() > 200 ? content.substring(0, 200) + "…" : content;
        String message = "#" + order.getId() + " · " + preview;

        Notification unread = repository.findFirstByClientIdAndTypeAndOrderIdAndReadFalse(
                clientId, NotificationType.ORDER_CHAT_MESSAGE, order.getId());

        if (unread != null) {
            unread.setMessage(message);
            unread.setCreatedAt(LocalDateTime.now());
            repository.save(unread);
            return;
        }

        create(clientId, NotificationType.ORDER_CHAT_MESSAGE, title, message, order.getId());
    }
    // Bienvenida cuando se crea una cuenta
    @Transactional
    public void notifyWelcome(Client client) {
        create(client.getId(), NotificationType.WELCOME, "¡Bienvenido!",
                "Hola, " + client.getName() + " ahora eres usuario de TerraGalega ¡Bienvenido!", null);
    }

    // Cupón nuevo: al dueño si es personal, a todos los clientes si es general
    @Transactional
    public void notifyCoupon(Coupon coupon) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            return;
        }
        // Los que empiezan más adelante no avisan (no están disponibles todavía)
        if (coupon.getStartsAt() != null && coupon.getStartsAt().isAfter(LocalDateTime.now())) {
            return;
        }

        String title = "¡Nuevo cupón para ti!";
        String message = "Nuevo cupón disponible para ti, entra y descúbrelo.";

        if (coupon.getClient() != null) {
            create(coupon.getClient().getId(), NotificationType.COUPON, title, message, null);
        } else {
            for (Client client : clientRepository.findAll()) {
                create(client.getId(), NotificationType.COUPON, title, message, null);
            }
        }
    }

    // Anuncio nuevo del home: el título del anuncio es el título de la notificación
    @Transactional
    public void notifyAnnouncement(Announcement announcement) {
        for (Client client : clientRepository.findAll()) {
            create(client.getId(), NotificationType.ANNOUNCEMENT, announcement.getTitle(),
                    "Nueva publicación en el inicio, entra y descúbrela.", null);
        }
    }

    // ---------- Lectura (la usa NotificationController) ----------

    // mobile = true: sin los anuncios, que son solo de la web
    @Transactional(readOnly = true)
    public List<Notification> getMine(Integer clientId, boolean mobile) {
        if (mobile) {
            return repository.findTop30ByClientIdAndTypeNotOrderByCreatedAtDesc(clientId,
                    NotificationType.ANNOUNCEMENT);
        }
        return repository.findTop30ByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Transactional
    public void markRead(Integer clientId, Integer id) {
        Notification notification = repository.findByIdAndClientId(id, clientId);
        if (notification == null) {
            throw ApiException.notFound("La notificación no existe.");
        }
        notification.setRead(true);
        repository.save(notification);
    }

    @Transactional
    public void markAllRead(Integer clientId) {
        List<Notification> unread = repository.findByClientIdAndReadFalse(clientId);
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        repository.saveAll(unread);
    }

    // También se usa al borrar la cuenta del cliente
    @Transactional
    public void deleteAll(Integer clientId) {
        repository.deleteByClientId(clientId);
    }
}