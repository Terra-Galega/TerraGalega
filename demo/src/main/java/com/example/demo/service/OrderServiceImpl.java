package com.example.demo.service;

import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderDetail;
import com.example.demo.entities.OrderDetailAddOn;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.Product;
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

    @Override
    @Transactional
    public List<Order> getOrdersByClientId(Integer clientId) {
        return repository.findByClientIdOrderByCreatedAtDesc(clientId);
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

}