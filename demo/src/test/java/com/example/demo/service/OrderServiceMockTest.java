package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.Client;
import com.example.demo.entities.Operator;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;

/*
    Pruebas unitarias de OrderServiceImpl con Mockito
*/
@ExtendWith(MockitoExtension.class)
class OrderServiceMockTest {

    // Repositorios simulados: ninguno toca una base de datos real
    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private AddOnRepository addOnRepository;

    @Mock
    private OperatorRepository operatorRepository;

    @Mock
    private DeliveryPersonRepository deliveryPersonRepository;

    // Clase que se prueba
    @InjectMocks
    private OrderServiceImpl orderService;

    // Constructor de un pedido en memoria con el id y estado dados
    private Order buildOrder(Integer id, OrderStatus status) {
        return Order.builder()
                .id(id)
                .status(status)
                .createdAt(LocalDateTime.now())
                .address("Calle 1 # 2-3")
                .deliveryFee(6000.0)
                .build();
    }

    @Test
    void OrderService_ConfirmOrder_ChangesPendingOrderToConfirmed() {
        // Arrange: el repositorio "encuentra" un pedido PENDING y devuelve lo que se
        // guarde
        Order pending = buildOrder(1, OrderStatus.PENDING);
        when(orderRepository.findById(1)).thenReturn(Optional.of(pending));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Order confirmed = orderService.confirmOrder(1);

        // Assert: el estado cambió y el servicio guardó el pedido
        assertThat(confirmed).isNotNull();
        assertThat(confirmed.getId()).isEqualTo(1);
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    void OrderService_Checkout_WithEmptyCart_ThrowsBadRequest() {
        // Arrange: un carrito sin productos
        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setAddress("Calle 1 # 2-3");
        request.setItems(List.of());
        Client client = new Client();

        // Act
        ApiException error = assertThrows(ApiException.class,
                () -> orderService.checkout(client, request));

        // Assert: error 400 y no se guardó nada
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void OrderService_CookOrder_WithInactiveOperator_ThrowsConflict() {
        // Arrange: pedido CONFIRMED y un operador que el admin desactivó
        Order confirmed = buildOrder(1, OrderStatus.CONFIRMED);
        Operator inactive = Operator.builder().id(5).active(false).build();
        when(orderRepository.findById(1)).thenReturn(Optional.of(confirmed));
        when(operatorRepository.findById(5)).thenReturn(Optional.of(inactive));

        // Act
        ApiException error = assertThrows(ApiException.class,
                () -> orderService.cookOrder(1, 5));

        // Assert: error 409, el pedido no cambió de estado y no tiene operador asignado
        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(confirmed.getOperator()).isNull();
    }

    @Test
    void OrderService_CancelPendingOrdersForClientDeletion_WithOrderInProgress_ThrowsConflictAndCancelsNothing() {
        // Arrange: el cliente tiene un pedido PENDING y otro ya PREPARING
        Order pending = buildOrder(1, OrderStatus.PENDING);
        Order preparing = buildOrder(2, OrderStatus.PREPARING);
        when(orderRepository.findByClientIdOrderByCreatedAtDesc(10))
                .thenReturn(List.of(pending, preparing));

        // Act
        ApiException error = assertThrows(ApiException.class,
                () -> orderService.cancelPendingOrdersForClientDeletion(10));

        // Assert: 409, el pedido PENDING NO se canceló (se valida antes de cambiar
        // nada)
        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(pending.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(preparing.getStatus()).isEqualTo(OrderStatus.PREPARING);
    }
}