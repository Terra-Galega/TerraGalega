package com.example.demo.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import com.example.demo.entities.Category;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderDetail;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.Product;

/*
    Pruebas de la consulta con @Query de OrderRepository
    existsByDetailsProductIdAndStatusNot.
 */
@DataJpaTest
class OrderRepositoryQueriesTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    private Product tortilla;
    private Product limonada;
    private Product sinPedidos;

    // Constructor, guarda un producto activo de la categoría dada
    private Product saveProduct(String name, double price, Category category) {
        return productRepository.save(Product.builder()
                .name(name).price(price).category(category)
                .active(true).popular(false).build());
    }

    // Constructor, guarda un pedido con una sola línea del producto dado
    private Order saveOrderWith(Product product, OrderStatus status) {
        Order order = Order.builder()
                .status(status)
                .createdAt(LocalDateTime.now())
                .address("Calle 1 # 2-3")
                .deliveryFee(6000.0)
                .build();

        OrderDetail detail = OrderDetail.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .unitPrice(product.getPrice())
                .build();
        order.getDetails().add(detail);

        // Las líneas se guardan solas por el cascade del pedido
        return orderRepository.save(order);
    }

    // Antes de cada prueba: la tortilla solo está en un pedido DELIVERED,
    // la limonada solo está en un pedido PENDING y el tercer producto nunca se pidió
    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder().name("Comida").build());

        tortilla = saveProduct("Tortilla", 25000.0, category);
        limonada = saveProduct("Limonada", 8000.0, category);
        sinPedidos = saveProduct("Postre", 12000.0, category);

        saveOrderWith(tortilla, OrderStatus.DELIVERED);
        saveOrderWith(limonada, OrderStatus.PENDING);
    }

    @Test
    void OrderRepository_ExistsByProductAndStatusNot_ReturnsTrueWhenOrderWithOtherStatusExists() {
        // Arrange: la limonada solo está en un pedido PENDING

        // Act: se pregunta si tiene pedidos en un estado distinto de DELIVERED
        boolean exists = orderRepository
                .existsByDetailsProductIdAndStatusNot(limonada.getId(), OrderStatus.DELIVERED);

        // Assert: sí, el pedido PENDING cuenta
        assertThat(exists).isTrue();
    }

    @Test
    void OrderRepository_ExistsByProductAndStatusNot_ReturnsFalseWhenAllOrdersHaveThatStatus() {
        // Arrange: la tortilla solo está en un pedido DELIVERED

        // Act: se pregunta si tiene pedidos en un estado distinto de DELIVERED
        boolean exists = orderRepository
                .existsByDetailsProductIdAndStatusNot(tortilla.getId(), OrderStatus.DELIVERED);

        // Assert: no, todos sus pedidos están entregados
        assertThat(exists).isFalse();
    }

}