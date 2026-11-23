package com.example.demo.service;

import com.example.demo.dto.PointsDTOs.Quote;
import com.example.demo.entities.Client;
import com.example.demo.entities.Order;
import com.example.demo.entities.PointsMovement;
import com.example.demo.entities.PointsMovement.Kind;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.PointsMovementRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class PointsService {
    public static final int COP_PER_POINT = 300;
    private static final int COP_TO_EARN = 1000;
    @jakarta.persistence.PersistenceContext(unitName = "h2")
    private EntityManager em;
    private final PointsMovementRepository movements;

    public PointsService(PointsMovementRepository movements) {
        this.movements = movements;
    }

    // Bloquea solo la fila del cliente (H2 no admite FOR UPDATE sobre sus joins).
    // Todos los cambios al saldo y su movimiento se hacen en la transacción del pedido.
    Client lockClient(Integer id) {
        em.flush();
        em.createNativeQuery("select id from clients where id = ?1 for update")
                .setParameter(1, id).getSingleResult();
        Client client = em.find(Client.class, id);
        em.refresh(client);
        return client;
    }

    Quote quote(Client client, double total) {
        long required = (long) Math.ceil(total / COP_PER_POINT);
        return new Quote(client.isSubscribed(), client.getPointsBalance(), required, total,
                client.isSubscribed() && required > 0 && client.getPointsBalance() >= required);
    }

    void spend(Client client, Order order, long amount) {
        if (!client.isSubscribed() || amount < 1 || client.getPointsBalance() < amount) {
            throw ApiException.conflict("Necesitas Premium activo y puntos para cubrir todo el pedido.");
        }
        record(client, order, Kind.SPENT, -amount);
    }

    void earn(Order order) {
        if (!Boolean.TRUE.equals(order.getPremiumAtCheckout()) || order.getPointsSpent() != null) return;
        double eligible = order.getSubtotal() - (order.getDiscountAmount() == null ? 0 : order.getDiscountAmount());
        long amount = (long) Math.floor(eligible / COP_TO_EARN);
        if (amount > 0) record(lockClient(order.getClient().getId()), order, Kind.EARNED, amount);
    }

    void refund(Order order) {
        if (order.getPointsSpent() != null) {
            record(lockClient(order.getClient().getId()), order, Kind.RETURNED, order.getPointsSpent());
        }
    }

    private void record(Client client, Order order, Kind kind, long amount) {
        client.setPointsBalance(Math.addExact(client.getPointsBalance(), amount));
        movements.save(new PointsMovement(order, kind, amount));
    }
}
