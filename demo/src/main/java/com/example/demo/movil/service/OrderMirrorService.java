package com.example.demo.movil.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.example.demo.entities.AddOn;
import com.example.demo.entities.Client;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderDetail;
import com.example.demo.movil.dto.CartDTOs.MirrorCheckDTO;
import com.example.demo.movil.entity.OrderMirror;
import com.example.demo.movil.entity.OrderMirrorItem;
import com.example.demo.movil.repository.OrderMirrorRepository;
import com.example.demo.repository.OrderRepository;

/**
 * Copia de los pedidos de H2 en PostgreSQL. Solo va en un sentido (H2 ->
 * Postgres): nadie
 * edita la copia, así que no puede haber conflictos. Si Postgres falla en un
 * momento dado,
 * el pedido de H2 NO se bloquea; la copia se arregla sola en el próximo cambio
 * del pedido,
 * al reiniciar, o con POST /sync/orders.
 */
@Service
public class OrderMirrorService {

    private static final Logger log = LoggerFactory.getLogger(OrderMirrorService.class);

    @Autowired
    private OrderMirrorRepository repository;
    @Autowired
    private OrderRepository orderRepository;

    private final TransactionTemplate pgTx;

    public OrderMirrorService(@Qualifier("postgresTransactionManager") PlatformTransactionManager pgManager) {
        this.pgTx = new TransactionTemplate(pgManager);
    }

    // Se llama desde OrderServiceImpl después de cada cambio del pedido. Nunca
    // lanza.
    public void sync(Order order) {
        try {
            upsert(order);
        } catch (Exception e) {
            log.warn("No se pudo copiar el pedido {} a Postgres: {}", order.getId(), e.getMessage());
        }
    }

    // Reconstruye la copia de TODOS los pedidos
    @Transactional(readOnly = true)
    public int resyncAll() {
        int count = 0;
        for (Order order : orderRepository.findAll()) {
            upsert(order);
            count++;
        }
        return count;
    }

    // Compara H2 con Postgres: debe devolver mismatchedOrderIds vacío
    @Transactional(readOnly = true)
    public MirrorCheckDTO check() {
        List<Order> orders = orderRepository.findAll();
        Map<Integer, OrderMirror> mirrors = repository.findAll().stream()
                .collect(Collectors.toMap(OrderMirror::getOrderId, Function.identity()));

        List<Integer> mismatched = new ArrayList<>();
        for (Order o : orders) {
            OrderMirror m = mirrors.get(o.getId());
            if (m == null || !m.getStatus().equals(o.getStatus().name())
                    || Math.abs(m.getTotal() - o.getTotal()) > 0.01) {
                mismatched.add(o.getId());
            }
        }
        return new MirrorCheckDTO(orders.size(), mirrors.size(), mismatched);
    }

    private void upsert(Order order) {
        pgTx.executeWithoutResult(status -> {
            OrderMirror mirror = repository.findByOrderId(order.getId()).orElseGet(OrderMirror::new);
            fill(mirror, order);
            repository.save(mirror);
        });
    }

    private void fill(OrderMirror m, Order o) {
        Client client = o.getClient();

        m.setOrderId(o.getId());
        m.setClientId(client != null ? client.getId() : null);
        m.setClientName(client != null ? client.getName() + " " + client.getLastName() : null);
        m.setDeliveryPersonId(o.getDeliveryPerson() != null ? o.getDeliveryPerson().getId() : null);
        m.setStatus(o.getStatus().name());
        m.setAddress(o.getAddress());
        m.setSubtotal(o.getSubtotal());
        m.setDeliveryFee(o.getDeliveryFee() != null ? o.getDeliveryFee() : 0.0);
        m.setTotal(o.getTotal());
        m.setCreatedAt(o.getCreatedAt());
        m.setDeliveredAt(o.getDeliveredAt());
        m.setSyncedAt(LocalDateTime.now());

        m.getItems().clear(); // orphanRemoval: las líneas se reemplazan por las actuales
        for (OrderDetail d : o.getDetails()) {
            double addOnsPrice = d.getAddOns().stream().mapToDouble(AddOn::getPrice).sum();

            Map<String, Integer> grouped = new LinkedHashMap<>();
            for (AddOn a : d.getAddOns()) {
                grouped.merge(a.getName(), 1, Integer::sum);
            }
            String addOnsText = grouped.entrySet().stream()
                    .map(e -> "x" + e.getValue() + " " + e.getKey())
                    .collect(Collectors.joining(", "));

            OrderMirrorItem item = new OrderMirrorItem();
            item.setOrder(m);
            item.setProductId(d.getProduct().getId());
            item.setProductName(d.getProduct().getName());
            item.setQuantity(d.getQuantity());
            item.setUnitPrice(d.getUnitPrice());
            item.setAddOns(addOnsText.isEmpty() ? null : addOnsText);
            item.setAddOnsPrice(addOnsPrice);
            item.setLineTotal((d.getUnitPrice() + addOnsPrice) * d.getQuantity());
            m.getItems().add(item);
        }
    }
}