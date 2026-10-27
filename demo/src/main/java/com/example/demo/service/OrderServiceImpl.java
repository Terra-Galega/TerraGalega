package com.example.demo.service;

import com.example.demo.errors.ApiException;
import java.time.LocalDate;
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
import com.example.demo.dto.OrderDTO;
import com.example.demo.dto.OrderDetailDTO;
import com.example.demo.dto.OrderProductDTO;
import com.example.demo.dto.ProductDTO;
import com.example.demo.dto.CategoryDTO;
import com.example.demo.dto.AddOnOrderDTO;
import com.example.demo.dto.ClientOrderDTO;
import com.example.demo.dto.DeliveryPersonOrderDTO;
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

    // Valor fijo del domicilio, se cobra en cada pedido (el front tiene el mismo
    // valor)
    private static final double DELIVERY_FEE = 6000.0;

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

    // Antes de eliminar la cuenta: si algún pedido ya fue confirmado por el
    // restaurante y aún no termina, se bloquea la eliminación. Los pedidos
    // que el restaurante aún no confirma (PENDING) se cancelan.
    @Override
    @Transactional
    public void cancelPendingOrdersForClientDeletion(Integer clientId) {
        List<Order> orders = repository.findByClientIdOrderByCreatedAtDesc(clientId);

        for (Order order : orders) {
            OrderStatus status = order.getStatus();

            if (status != OrderStatus.PENDING && !FINISHED_STATUSES.contains(status)) {
                throw ApiException.conflict(
                        "No puedes eliminar tu cuenta mientras tengas pedidos en curso. "
                                + "Espera a que sean entregados o cancélalos si aún no han sido confirmados.");
            }
        }

        for (Order order : orders) {
            if (order.getStatus() == OrderStatus.PENDING) {
                order.setStatus(OrderStatus.CANCELLED);
            }
        }

        repository.saveAll(orders);
    }

    @Override
    @Transactional
    public Order getOrderById(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("El pedido no existe."));
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
            throw ApiException.badRequest("El carrito está vacío.");
        }

        Order order = Order.builder()
                .client(client)
                .status(OrderStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .address(request.getAddress())
                .deliveryFee(DELIVERY_FEE)
                .build();

        List<OrderDetail> details = new ArrayList<>();

        for (CheckoutItemDTO item : request.getItems()) {

            Integer quantity = item.getQuantity();
            if (quantity == null || quantity < 1) {
                throw ApiException.badRequest(
                        "Cantidad inválida para el producto " + item.getProductId());
            }

            Product product = productRepository.findById(item.getProductId())
                    .orElseThrow(() -> ApiException.conflict(
                            "Uno de los productos de tu carrito ya no está disponible."));

            // El producto pudo desactivarse después de que el cliente lo guardara en su
            // carrito
            if (!Boolean.TRUE.equals(product.getActive())) {
                throw ApiException.conflict(
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
                            .orElseThrow(() -> ApiException.conflict(
                                    "Adicional no encontrado: " + addOnId));

                    boolean addOnAllowed = product.getCategory() != null
                            && addOn.getCategories().stream()
                                    .anyMatch(c -> c.getId().equals(product.getCategory().getId()));

                    if (!addOnAllowed) {
                        throw ApiException.badRequest(
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
        return repository.findByStatusNotInOrderByCreatedAtAsc(FINISHED_STATUSES).stream()
                .map(this::toSummary)
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
            throw ApiException.conflict("Solo se pueden confirmar pedidos pendientes.");
        }

        order.setStatus(OrderStatus.CONFIRMED);

        return repository.save(order);
    }

    @Override
    @Transactional
    public Order cancelOrder(Integer orderId, Integer clientId) {

        Order order = getOrderById(orderId);

        // Solo el dueño del pedido puede cancelarlo
        if (order.getClient() == null || !order.getClient().getId().equals(clientId)) {
            throw ApiException.forbidden("Este pedido no te pertenece.");
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw ApiException.conflict(
                    "Solo puedes cancelar pedidos que el restaurante aún no ha confirmado.");
        }

        order.setStatus(OrderStatus.CANCELLED);

        return repository.save(order);
    }

    @Override
    @Transactional
    public Order cookOrder(Integer orderId, Integer operatorId) {

        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw ApiException.conflict("Solo se pueden cocinar pedidos confirmados.");
        }

        if (operatorId == null) {
            throw ApiException.badRequest("Falta el operador que cocina el pedido.");
        }

        Operator operator = operatorRepository.findById(operatorId)
                .orElseThrow(() -> ApiException.notFound("El operador no existe."));

        if (!Boolean.TRUE.equals(operator.getActive())) {
            throw ApiException.conflict("El operador está desactivado.");
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
            throw ApiException.conflict("Solo se pueden enviar pedidos que se están preparando.");
        }

        if (deliveryPersonId == null) {
            throw ApiException.badRequest("Selecciona un domiciliario.");
        }

        DeliveryPerson deliveryPerson = deliveryPersonRepository.findById(deliveryPersonId)
                .orElseThrow(() -> ApiException.notFound("El domiciliario no existe."));

        if (!Boolean.TRUE.equals(deliveryPerson.getActive())
                || !Boolean.TRUE.equals(deliveryPerson.getAvailable())) {
            throw ApiException.conflict("El domiciliario no está disponible.");
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
            throw ApiException.conflict("Solo se pueden completar pedidos que van en camino.");
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

    @Override
    @Transactional
    public OrderDTO toDTO(Order order) {
        return buildDTO(order, false);
    }

    @Override
    @Transactional
    public OrderDTO toDTOWithContact(Order order) {
        return buildDTO(order, true);
    }

    private OrderDTO buildDTO(Order order, boolean includeContact) {

        // Volver a cargar el pedido dentro de la transacción
        order = repository.findById(order.getId())
                .orElseThrow(() -> ApiException.notFound("El pedido no existe."));

        OrderDTO dto = new OrderDTO();

        dto.setId(order.getId());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setDeliveredAt(order.getDeliveredAt());
        dto.setAddress(order.getAddress());
        dto.setTotal(order.getTotal());

        // Cliente y domiciliario: solo cuando la pantalla los necesita
        if (includeContact) {
            if (order.getClient() != null) {
                ClientOrderDTO clientDTO = new ClientOrderDTO();
                clientDTO.setId(order.getClient().getId());
                clientDTO.setName(order.getClient().getName());
                clientDTO.setLastName(order.getClient().getLastName());
                clientDTO.setEmail(order.getClient().getEmail());
                clientDTO.setPhone(order.getClient().getPhone());
                dto.setClient(clientDTO);
            }

            if (order.getDeliveryPerson() != null) {
                DeliveryPersonOrderDTO deliveryDTO = new DeliveryPersonOrderDTO();
                deliveryDTO.setId(order.getDeliveryPerson().getId());
                deliveryDTO.setName(order.getDeliveryPerson().getName());
                deliveryDTO.setLastName(order.getDeliveryPerson().getLastName());
                deliveryDTO.setPhone(order.getDeliveryPerson().getPhone());
                dto.setDeliveryPerson(deliveryDTO);
            }
        }

        // Detalles del pedido
        List<OrderDetailDTO> detailDTOs = new ArrayList<>();

        for (OrderDetail detail : order.getDetails()) {
            OrderDetailDTO detailDTO = new OrderDetailDTO();

            detailDTO.setId(detail.getId());
            detailDTO.setQuantity(detail.getQuantity());
            detailDTO.setUnitPrice(detail.getUnitPrice());

            // Producto: solo lo que muestra la interfaz
            Product product = detail.getProduct();
            OrderProductDTO productDTO = new OrderProductDTO();
            productDTO.setId(product.getId());
            productDTO.setName(product.getName());
            productDTO.setImageUrl(product.getImageUrl());
            detailDTO.setProduct(productDTO);

            // Adicionales seleccionados para este producto
            List<AddOnOrderDTO> addOnDTOs = new ArrayList<>();

            for (AddOn addOn : detail.getAddOns()) {
                AddOnOrderDTO addOnDTO = new AddOnOrderDTO();
                addOnDTO.setId(addOn.getId());
                addOnDTO.setName(addOn.getName());
                addOnDTO.setPrice(addOn.getPrice());
                addOnDTOs.add(addOnDTO);
            }

            detailDTO.setAddOns(addOnDTOs);
            detailDTOs.add(detailDTO);
        }

        dto.setDetails(detailDTOs);

        return dto;
    }

    // Filtros opcionales y combinables: rango de fechas (ambos extremos incluidos),
    // pedidos que contienen un producto, y número de pedido.
    @Override
    @Transactional
    public List<OrderSummaryDTO> searchOrderSummaries(
            LocalDate from, LocalDate to, Integer productId, Integer orderId) {

        if (from != null && to != null && from.isAfter(to)) {
            throw ApiException.badRequest(
                    "La fecha inicial no puede ser posterior a la fecha final.");
        }

        LocalDateTime start = from != null ? from.atStartOfDay() : null;
        LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;

        return repository.findAllByOrderByCreatedAtDesc().stream()
                .filter(o -> orderId == null || o.getId().equals(orderId))
                .filter(o -> start == null || !o.getCreatedAt().isBefore(start))
                .filter(o -> end == null || o.getCreatedAt().isBefore(end))
                .filter(o -> productId == null || o.getDetails().stream()
                        .anyMatch(d -> d.getProduct().getId().equals(productId)))
                .map(this::toSummary)
                .toList();
    }
}