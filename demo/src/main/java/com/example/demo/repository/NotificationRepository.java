package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.demo.entities.Notification;
import com.example.demo.entities.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Integer> {

    // Las 30 más recientes del cliente (web: todas)
    List<Notification> findTop30ByClientIdOrderByCreatedAtDesc(Integer clientId);

    // Las 30 más recientes sin un tipo (móvil: sin los anuncios)
    List<Notification> findTop30ByClientIdAndTypeNotOrderByCreatedAtDesc(Integer clientId, NotificationType type);

    // Para no repetir el aviso de un mismo pedido
    boolean existsByClientIdAndTypeAndOrderId(Integer clientId, NotificationType type, Integer orderId);

    // Una notificación, solo si es del cliente
    Notification findByIdAndClientId(Integer id, Integer clientId);

    List<Notification> findByClientIdAndReadFalse(Integer clientId);

    void deleteByClientId(Integer clientId);
    // Aviso de chat aún sin leer de un pedido (para actualizarlo en vez de crear otro)
    Notification findFirstByClientIdAndTypeAndOrderIdAndReadFalse(Integer clientId, NotificationType type,Integer orderId);
}