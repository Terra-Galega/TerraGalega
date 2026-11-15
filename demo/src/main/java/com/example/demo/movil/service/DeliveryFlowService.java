package com.example.demo.movil.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.movil.dto.DeliveryDTOs.ClientDeliveryDTO;
import com.example.demo.movil.dto.DeliveryDTOs.DeliveryJobDTO;
import com.example.demo.movil.entity.DeliveryTracking;
import com.example.demo.dto.OrderDTO;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.OrderService;

import com.example.demo.entities.NotificationType;
import com.example.demo.service.NotificationService;

import com.example.demo.movil.dto.DeliveryDTOs.MapPointDTO;
import com.example.demo.movil.dto.DeliveryDTOs.OperatorDeliveryDTO;

/**
 * Une las dos bases: el pedido sale de H2 y el PIN/ubicación de Postgres.
 * Sin @Transactional a propósito: no existe una transacción única que cubra
 * dos bases; cada llamada abre la suya.
 */
@Service
public class DeliveryFlowService {

    @Autowired
    private DeliveryTrackingService tracking;

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    // Avance del recorrido desde el cual se avisa "ya va llegando" (85 %)
    private static final double NEAR_PROGRESS = 0.85;

    @Autowired
    private NotificationService notificationService;

    // ---------- CLIENTE ----------

    public List<ClientDeliveryDTO> activeForClient(Integer clientId) {
        return orderRepository.findByClientIdAndStatus(clientId, OrderStatus.ON_THE_WAY).stream()
                .map(order -> toClientDTO(order, tracking.findOrCreate(order)))
                .toList();
    }

    public ClientDeliveryDTO forClient(Integer clientId, Integer orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order.getClient() == null || !order.getClient().getId().equals(clientId)) {
            throw ApiException.forbidden("Este pedido no pertenece a este cliente.");
        }
        return toClientDTO(order, tracking.findOrCreate(order));
    }

    // ---------- REPARTIDOR ----------

    public List<DeliveryJobDTO> activeForDeliveryPerson(Integer deliveryPersonId) {
        return orderRepository.findByDeliveryPersonIdAndStatus(deliveryPersonId, OrderStatus.ON_THE_WAY).stream()
                .map(order -> toJobDTO(order, tracking.findOrCreate(order)))
                .toList();
    }

    public DeliveryJobDTO updateLocation(Integer deliveryPersonId, Integer orderId,
            Double latitude, Double longitude) {
        if (latitude == null || longitude == null
                || latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw ApiException.badRequest("Ubicación inválida.");
        }
        Order order = onTheWayOf(deliveryPersonId, orderId);
        tracking.findOrCreate(order);
        DeliveryTracking updated = tracking.updateLocation(orderId, latitude, longitude);
        Double progress = tracking.progressOf(updated);
        if (progress != null && progress >= NEAR_PROGRESS) {
            notificationService.notifyOrder(order, NotificationType.ORDER_NEAR);
        }
        return toJobDTO(order, updated);
    }

    // Valida el PIN, marca el pedido como entregado en H2 y cierra el
    // seguimiento en Postgres.
    public OrderDTO complete(Integer deliveryPersonId, Integer orderId, String pin) {
        Order order = onTheWayOf(deliveryPersonId, orderId);
        tracking.findOrCreate(order);
        tracking.verifyPin(orderId, pin); // error si el PIN no coincide
        Order delivered = orderService.deliverOrder(orderId); // H2: DELIVERED + libera al repartidor
        tracking.markDelivered(orderId); // Postgres
        return orderService.toDTO(delivered);
    }

    // ---------- helpers ----------

    private Order onTheWayOf(Integer deliveryPersonId, Integer orderId) {
        Order order = orderService.getOrderById(orderId);
        if (order.getDeliveryPerson() == null || !order.getDeliveryPerson().getId().equals(deliveryPersonId)) {
            throw ApiException.forbidden("Este pedido no está asignado a este domiciliario.");
        }
        if (order.getStatus() != OrderStatus.ON_THE_WAY) {
            throw ApiException.conflict("El pedido ya no está en camino.");
        }
        return order;
    }

    private ClientDeliveryDTO toClientDTO(Order order, DeliveryTracking t) {
        DeliveryPerson dp = order.getDeliveryPerson();
        boolean onTheWay = order.getStatus() == OrderStatus.ON_THE_WAY;

        return new ClientDeliveryDTO(
                order.getId(),
                order.getStatus().name(),
                order.getAddress(),
                onTheWay ? t.getPin() : null,
                dp != null ? dp.getName() + " " + dp.getLastName() : null,
                dp != null ? dp.getPhone() : null,
                t.getLatitude(),
                t.getLongitude(),
                t.getLocationUpdatedAt(),
                t.getStartedAt(),
                tracking.routeOf(t),
                onTheWay ? tracking.progressOf(t) : null);
    }

    private DeliveryJobDTO toJobDTO(Order order, DeliveryTracking t) {
        var client = order.getClient();

        return new DeliveryJobDTO(
                order.getId(),
                order.getStatus().name(),
                order.getAddress(),
                client != null ? client.getName() + " " + client.getLastName() : null,
                client != null ? client.getPhone() : null,
                t.getLatitude(),
                t.getLongitude(),
                t.getLocationUpdatedAt(),
                Math.max(
                        0,
                        DeliveryTrackingService.MAX_PIN_ATTEMPTS
                                - t.getFailedAttempts()),
                t.getStartedAt(),
                tracking.routeOf(t));
    }

    // El repartidor inicia únicamente un pedido asignado a él.
    public DeliveryJobDTO start(
            Integer deliveryPersonId,
            Integer orderId,
            List<MapPointDTO> route) {

        Order order = onTheWayOf(deliveryPersonId, orderId);

        tracking.findOrCreate(order);

        DeliveryTracking updated = tracking.beginTrip(orderId, route);
        notificationService.notifyOrder(order, NotificationType.ORDER_ON_THE_WAY);

        return toJobDTO(order, updated);
    }

    // Igual que la pantalla actual del operador:
    // puede consultar los pedidos activos del restaurante.
    public List<OperatorDeliveryDTO> activeForOperator() {
        return orderRepository
                .findByStatusOrderByCreatedAtAsc(OrderStatus.ON_THE_WAY)
                .stream()
                .flatMap(order -> tracking.findStarted(order.getId())
                        .filter(t -> order.getDeliveryPerson() != null
                                && order.getDeliveryPerson().getId()
                                        .equals(t.getDeliveryPersonId()))
                        .map(t -> toOperatorDTO(order, t))
                        .stream())
                .toList();
    }

    private OperatorDeliveryDTO toOperatorDTO(
            Order order,
            DeliveryTracking trackingData) {

        DeliveryPerson person = order.getDeliveryPerson();

        return new OperatorDeliveryDTO(
                order.getId(),
                order.getAddress(),
                person.getName() + " " + person.getLastName(),
                person.getPhone(),
                trackingData.getLatitude(),
                trackingData.getLongitude(),
                trackingData.getLocationUpdatedAt(),
                trackingData.getStartedAt(),
                tracking.routeOf(trackingData));
    }
}