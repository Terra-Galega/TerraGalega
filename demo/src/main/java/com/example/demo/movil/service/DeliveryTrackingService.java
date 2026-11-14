package com.example.demo.movil.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.movil.entity.DeliveryTracking;
import com.example.demo.movil.entity.DeliveryTrackingStatus;
import com.example.demo.movil.repository.DeliveryTrackingRepository;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import com.example.demo.errors.ApiException;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.example.demo.movil.dto.DeliveryDTOs.MapPointDTO;

@Service
public class DeliveryTrackingService {

    public static final int MAX_PIN_ATTEMPTS = 5;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String TX = "postgresTransactionManager";

    @Autowired
    private DeliveryTrackingRepository repository;

    // Crea el seguimiento y su PIN cuando el pedido sale (o lo reasigna si se
    // vuelve a enviar antes de entregarse).
    @Transactional(TX)
    public DeliveryTracking startTracking(Order order) {
        if (order.getDeliveryPerson() == null) {
            throw ApiException.badRequest("El pedido no tiene domiciliario asignado.");
        }

        DeliveryTracking tracking = repository.findByOrderId(order.getId())
                .orElseGet(DeliveryTracking::new);

        if (tracking.getStatus() == DeliveryTrackingStatus.DELIVERED) {
            return tracking;
        }

        tracking.setOrderId(order.getId());
        tracking.setClientId(order.getClient() != null ? order.getClient().getId() : null);
        tracking.setDeliveryPersonId(order.getDeliveryPerson().getId());
        tracking.setPin(newPin());
        tracking.setStatus(DeliveryTrackingStatus.IN_PROGRESS);
        tracking.setFailedAttempts(0);
        tracking.setLatitude(null);
        tracking.setLongitude(null);
        tracking.setLocationUpdatedAt(null);
        tracking.setStartedAt(null);
        tracking.setRoutePath(null);
        if (tracking.getCreatedAt() == null) {
            tracking.setCreatedAt(LocalDateTime.now());
        }
        return repository.save(tracking);
    }

    // Devuelve el seguimiento; si el pedido ya iba en camino antes de existir
    // esta tabla (datos de prueba), lo crea en ese momento.
    @Transactional(TX)
    public DeliveryTracking findOrCreate(Order order) {
        return repository.findByOrderId(order.getId()).orElseGet(() -> {
            if (order.getStatus() != OrderStatus.ON_THE_WAY || order.getDeliveryPerson() == null) {
                throw ApiException.notFound("Este pedido no tiene seguimiento de domicilio.");
            }
            return startTracking(order);
        });
    }

    @Transactional(TX)
    public DeliveryTracking updateLocation(Integer orderId, Double latitude, Double longitude) {
        DeliveryTracking tracking = get(orderId);
        if (tracking.getStartedAt() == null) {
            throw ApiException.conflict(
                    "Debes iniciar la entrega antes de enviar tu ubicación.");
        }

        if (tracking.getStatus() != DeliveryTrackingStatus.IN_PROGRESS) {
            throw ApiException.conflict("El seguimiento ya no está activo.");
        }
        tracking.setLatitude(latitude);
        tracking.setLongitude(longitude);
        tracking.setLocationUpdatedAt(LocalDateTime.now());
        return repository.save(tracking);
    }

    // noRollbackFor: si el PIN es incorrecto se lanza una excepción, pero el
    // intento fallido tiene que quedar guardado igual.
    @Transactional(value = TX, noRollbackFor = ApiException.class)
    public void verifyPin(Integer orderId, String pin) {
        DeliveryTracking tracking = get(orderId);

        if (tracking.getStatus() == DeliveryTrackingStatus.DELIVERED) {
            throw ApiException.conflict("Este pedido ya fue entregado.");
        }
        if (tracking.getFailedAttempts() >= MAX_PIN_ATTEMPTS) {
            throw ApiException.forbidden("PIN bloqueado por demasiados intentos. Contacta al operador.");
        }

        boolean ok = pin != null && MessageDigest.isEqual(
                pin.trim().getBytes(StandardCharsets.UTF_8),
                tracking.getPin().getBytes(StandardCharsets.UTF_8));

        if (!ok) {
            tracking.setFailedAttempts(tracking.getFailedAttempts() + 1);
            repository.save(tracking);
            int left = MAX_PIN_ATTEMPTS - tracking.getFailedAttempts();
            throw ApiException.badRequest(left > 0
                    ? "PIN incorrecto. Te quedan " + left + " intentos."
                    : "PIN bloqueado por demasiados intentos. Contacta al operador.");
        }
    }

    @Transactional(TX)
    public void markDelivered(Integer orderId) {
        DeliveryTracking tracking = get(orderId);
        tracking.setStatus(DeliveryTrackingStatus.DELIVERED);
        tracking.setDeliveredAt(LocalDateTime.now());
        repository.save(tracking);
    }

    private DeliveryTracking get(Integer orderId) {
        return repository.findByOrderId(orderId)
                .orElseThrow(() -> ApiException.notFound("Este pedido no tiene seguimiento de domicilio."));
    }

    // PIN de 4 dígitos, con ceros a la izquierda (0042)
    private String newPin() {
        return String.format("%04d", RANDOM.nextInt(10_000));
    }

    // Registra el inicio real del recorrido.
    // Reintentar la misma solicitud no reinicia una entrega ya iniciada.
    @Transactional(TX)
    public DeliveryTracking beginTrip(
            Integer orderId,
            List<MapPointDTO> route) {

        DeliveryTracking tracking = get(orderId);

        if (tracking.getStatus() != DeliveryTrackingStatus.IN_PROGRESS) {
            throw ApiException.conflict("El seguimiento ya no está activo.");
        }

        if (tracking.getStartedAt() != null) {
            return tracking;
        }

        validateRoute(route);

        String encoded = route.stream()
                .map(point -> point.lat() + "," + point.lon())
                .collect(Collectors.joining(";"));

        tracking.setRoutePath(encoded);
        tracking.setStartedAt(LocalDateTime.now());

        return repository.save(tracking);
    }

    // Consulta sin crear seguimientos nuevos.
    @Transactional(value = TX, readOnly = true)
    public Optional<DeliveryTracking> findStarted(Integer orderId) {
        return repository.findByOrderId(orderId)
                .filter(tracking -> tracking.getStartedAt() != null);
    }

    // Convierte la ruta almacenada en puntos para los DTO.
    public List<MapPointDTO> routeOf(DeliveryTracking tracking) {
        String encoded = tracking.getRoutePath();

        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }

        return Arrays.stream(encoded.split(";"))
                .map(value -> {
                    String[] coordinates = value.split(",");
                    return new MapPointDTO(
                            Double.valueOf(coordinates[0]),
                            Double.valueOf(coordinates[1]));
                })
                .toList();
    }

    private void validateRoute(List<MapPointDTO> route) {
        if (route == null || route.size() < 2 || route.size() > 10000) {
            throw ApiException.badRequest(
                    "La ruta debe contener entre 2 y 10000 puntos.");
        }

        for (MapPointDTO point : route) {
            if (point == null
                    || point.lat() == null
                    || point.lon() == null
                    || !Double.isFinite(point.lat())
                    || !Double.isFinite(point.lon())
                    || point.lat() < -90
                    || point.lat() > 90
                    || point.lon() < -180
                    || point.lon() > 180) {

                throw ApiException.badRequest(
                        "La ruta contiene coordenadas inválidas.");
            }
        }
    }
}