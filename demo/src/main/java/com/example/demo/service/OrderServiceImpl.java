package com.example.demo.service;

import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.dto.OrderSummaryDTO;
import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderDetail;
import com.example.demo.entities.OrderDetailAddOn;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.Product;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Operator;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.entities.Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository repository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddOnRepository addOnRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    private static final List<OrderStatus> FINISHED_STATUSES = List.of(
            OrderStatus.DELIVERED,
            OrderStatus.CANCELLED);

    @Override
    @Transactional
    public List<Order> getOrdersByClientId(Integer clientId) {
        return repository.findByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Override
    @Transactional
    public List<Order> getAllOrders() {
        return repository.findAll(org.springframework.data.domain.Sort
                .by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
    }

    @Override
    @Transactional
    public void clearClientIdFromOrders(Integer clientId) {
        List<Order> orders = repository.findByClientIdOrderByCreatedAtDesc(clientId);
        for (Order order : orders) {
            order.setClient(null);
        }
        repository.saveAll(orders);
    }

    @Override
    @Transactional
    public Order getOrderById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("El pedido no existe."));
    }

    @Override
    @Transactional
    public Order createOrder(Order order) {
        return repository.save(order);
    }

    // Arma el pedido a partir del carrito recibido desde /checkout y lo
    // guarda en estado PENDING. El precio de cada producto y adicional se
    // vuelve a consultar en el servidor: nunca se confía en los precios
    // que manda el navegador.
    @Override
    @Transactional
    public Order checkout(Client client, CheckoutRequestDTO request) {

        if (request.getItems() == null || request.getItems().isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío.");
        }

        Order order = Order.builder()
                .client(client)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .address(request.getAddress())
                .build();

        List<OrderDetail> details = new ArrayList<>();

        for (CheckoutItemDTO item : request.getItems()) {

            Integer quantity = item.getQuantity();
            if (quantity == null || quantity < 1) {
                throw new IllegalArgumentException(
                        "Cantidad inválida para el producto " + item.getProductId());
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Producto no encontrado: " + item.getProductId()));

            // El producto pudo desactivarse después de que el cliente lo guardara en su
            // carrito
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw new IllegalArgumentException(
                        "El producto \"" + product.getName() + "\" ya no está disponible.");
            }

            OrderDetail detail = OrderDetail.builder()
                    .order(order)
                    .product(product)
                    .quantity(quantity)
                    .unitPrice(product.getPrice())
                    .build();

            List<OrderDetailAddOn> orderDetailAddOns = new ArrayList<>();

            if (item.getAddOnIds() != null) {
                for (Integer addOnId : item.getAddOnIds()) {
                    AddOn addOn = addOnRepository.findById(addOnId)
                            .orElseThrow(() -> new IllegalArgumentException(
                                    "Adicional no encontrado: " + addOnId));

                    boolean addOnAllowed = product.getCategory() != null
                            && addOn.getCategories().stream()
                                    .anyMatch(c -> c.getId().equals(product.getCategory().getId()));

                    if (!addOnAllowed) {
                        throw new IllegalArgumentException(
                                "El adicional seleccionado no está disponible para este producto.");
                    }

                    orderDetailAddOns.add(
                            OrderDetailAddOn.builder()
                                    .orderDetail(detail)
                                    .addOn(addOn)
                                    .build());
                }
            }

            detail.setOrderDetailAddOns(orderDetailAddOns);
            details.add(detail);
        }

        order.setDetails(details);

        return repository.save(order);
    }

    @Override
    @Transactional
    public List<OrderSummaryDTO> getActiveOrders() {
        return repository.findByStatusNotInOrderByCreatedAtDesc(FINISHED_STATUSES).stream().map(this::toSummary)
                .toList();
    }

    @Override
    @Transactional
    public List<OrderSummaryDTO> getAllOrderSummaries() {
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    // Datos planos para la tabla del operador; el detalle se pide aparte con GET
    // /orders/{id}
    private OrderSummaryDTO toSummary(Order order) {
        Client client = order.getClient();
        DeliveryPerson deliveryPerson = order.getDeliveryPerson();

        return new OrderSummaryDTO(
                order.getId(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getAddress(),
                client != null ? client.getName() + " " + client.getLastName() : null,
                deliveryPerson != null
                        ? deliveryPerson.getName() + " " + deliveryPerson.getLastName()
                        : null,
                order.getTotal());
    }

    @Override
    @Transactional
    public Order confirmOrder(Integer orderId) {

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("Solo se pueden confirmar pedidos pendientes.");
        }

        order.setStatus(OrderStatus.CONFIRMED);

        return repository.save(order);
    }

    @Override
    @Transactional
    public Order cookOrder(Integer orderId, Integer operatorId) {

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new IllegalArgumentException("Solo se pueden cocinar pedidos confirmados.");
        }

        if (operatorId == null) {
            throw new IllegalArgumentException("Falta el operador que cocina el pedido.");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> new IllegalArgumentException("El operador no existe."));

        if (!Boolean.TRUE.equals(operator.getActive())) {
            throw new IllegalArgumentException("El operador está desactivado.");
        }

        order.setOperator(operator);
        order.setStatus(OrderStatus.PREPARING);

        return repository.save(order);
    }

    @Override
    @Transactional
    public Order sendOrder(Integer orderId, Integer deliveryPersonId) {

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.PREPARING) {
            throw new IllegalArgumentException("Solo se pueden enviar pedidos que se están preparando.");
        }

        if (deliveryPersonId == null) {
            throw new IllegalArgumentException("Selecciona un domiciliario.");
        }

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(deliveryPersonId)
                .orElseThrow(() -> new IllegalArgumentException("El domiciliario no existe."));

        if (!Boolean.TRUE.equals(deliveryPerson.getActive())
                || !Boolean.TRUE.equals(deliveryPerson.getAvailable())) {
            throw new IllegalArgumentException("El domiciliario no está disponible.");
        }

        // Mientras lleva el pedido no puede recibir otro
        deliveryPerson.setAvailable(false);
        deliveryPersonRepository.save(deliveryPerson);

        order.setDeliveryPerson(deliveryPerson);
        order.setStatus(OrderStatus.ON_THE_WAY);

        return repository.save(order);
    }

    @Override
    @Transactional
    public Order deliverOrder(Integer orderId) {

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.ON_THE_WAY) {
            throw new IllegalArgumentException("Solo se pueden completar pedidos que van en camino.");
        }

        order.setStatus(OrderStatus.DELIVERED);
        order.setDeliveredAt(LocalDateTime.now());

        // El domiciliario queda libre para otro pedido
        DeliveryPerson deliveryPerson = order.getDeliveryPerson();
        if (deliveryPerson != null) {
            deliveryPerson.setAvailable(true);
            deliveryPersonRepository.save(deliveryPerson);
        }

        return repository.save(order);
    }
}