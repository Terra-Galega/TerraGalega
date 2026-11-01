package com.example.demo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.dto.CheckoutRequestDTO;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Category;
import com.example.demo.entities.Client;
import com.example.demo.entities.DeliveryPerson;
import com.example.demo.entities.Operator;
import com.example.demo.entities.Order;
import com.example.demo.entities.OrderStatus;
import com.example.demo.entities.Product;
import com.example.demo.entities.Role;
import com.example.demo.errors.ApiException;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.DeliveryPersonRepository;
import com.example.demo.repository.OperatorRepository;
import com.example.demo.repository.ProductRepository;

/*
    Pruebas de integración de OrderService: usan la aplicación completa y la base de "test", 
 */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AddOnRepository addOnRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private OperatorRepository operatorRepository;

    @Autowired
    private DeliveryPersonRepository deliveryPersonRepository;

    // variables de prueba llenadas en setUp()
    private Product tortilla;
    private Product tortillaInactiva;
    private AddOn quesoExtra;
    private AddOn hielo;
    private Client client;
    private Client otroClient;
    private Operator operator;
    private DeliveryPerson deliveryPerson;

    // Construye y guarda un cliente válido; el sufijo hace únicos email y teléfono
    private Client saveClient(String name, String suf) {
        return clientRepository.save(Client.builder()
                .name(name)
                .lastName("Prueba")
                .email("integracion" + suf + "@test.com")
                .password("Clave123*")
                .role(Role.CLIENT)
                .phone("31000000" + suf)
                .address("Calle 1 # 2-3")
                .build());
    }

    // (constructorr) Arma un carrito de un solo producto con la cantidad y adicionales dados
    private CheckoutRequestDTO buildRequest(Integer productId, Integer quantity, List<Integer> addOnIds) {
        CheckoutItemDTO item = new CheckoutItemDTO();
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setAddOnIds(addOnIds);

        CheckoutRequestDTO request = new CheckoutRequestDTO();
        request.setAddress("Carrera 7 # 45-10");
        request.setItems(List.of(item));
        return request;
    }

    // Antes de cada prueba: categorías, productos, adicionales y personas.
    @BeforeEach
    void setUp() {
        Category tortillas = categoryRepository.save(Category.builder().name("Tortillas Test").build());
        Category bebidas = categoryRepository.save(Category.builder().name("Bebidas Test").build());

        tortilla = productRepository.save(Product.builder()
                .name("Tortilla Test").price(25000.0).category(tortillas)
                .active(true).popular(false).build());
        tortillaInactiva = productRepository.save(Product.builder()
                .name("Tortilla Inactiva Test").price(20000.0).category(tortillas)
                .active(false).popular(false).build());

        // El queso extra solo está permitido para la categoría Tortillas
        quesoExtra = addOnRepository.save(AddOn.builder()
                .name("Queso extra Test").price(3000.0).active(true)
                .categories(Set.of(tortillas)).build());
        // El hielo solo está permitido para la categoría Bebidas
        hielo = addOnRepository.save(AddOn.builder()
                .name("Hielo Test").price(500.0).active(true)
                .categories(Set.of(bebidas)).build());

        client = saveClient("Laura", "01");
        otroClient = saveClient("Mateo", "02");

        operator = operatorRepository.save(Operator.builder()
                .name("Olga").lastName("Prueba").email("operador.test@test.com")
                .password("Clave123*").role(Role.OPERATOR).username("operador_test")
                .active(true).build());

        deliveryPerson = deliveryPersonRepository.save(DeliveryPerson.builder()
                .name("Diego").lastName("Prueba").email("domiciliario.test@test.com")
                .password("Clave123*").role(Role.DELIVERY_PERSON)
                .identification("9001").phone("3009000001")
                .available(true).active(true).build());
    }

    @Test
    void OrderService_Checkout_WithValidCart_CreatesPendingOrderWithCorrectTotal() {
        // Arrange: 2 tortillas con queso extra permitido para su categoría
        CheckoutRequestDTO request = buildRequest(tortilla.getId(), 2, List.of(quesoExtra.getId()));

        // Act
        Order created = orderService.checkout(client, request);

        // Assert: queda PENDING, con una línea que copia el precio del producto
        assertThat(created.getId()).isNotNull();
        assertThat(created.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(created.getCreatedAt()).isNotNull();
        assertThat(created.getDetails()).hasSize(1);
        assertThat(created.getDetails().get(0).getQuantity()).isEqualTo(2);
        assertThat(created.getDetails().get(0).getUnitPrice()).isEqualTo(25000.0);

        // Total = (25000 + 3000 del queso) * 2 + 6000 de domicilio = 62000
        assertThat(created.getTotal()).isEqualTo(62000.0);
    }

    @Test
    void OrderService_Checkout_WithInactiveProduct_ThrowsConflict() {
        // Arrange: el cliente guardó en su carrito un producto que luego se desactivó
        CheckoutRequestDTO request = buildRequest(tortillaInactiva.getId(), 1, null);

        // Act
        ApiException error = assertThrows(ApiException.class,
                () -> orderService.checkout(client, request));

        // Assert
        assertThat(error.getStatus()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void OrderService_Checkout_WithAddOnFromAnotherCategory_ThrowsBadRequest() {
        // Arrange: se pide una tortilla con hielo, adicional que solo es de Bebidas
        CheckoutRequestDTO request = buildRequest(tortilla.getId(), 1, List.of(hielo.getId()));

        // Act
        ApiException error = assertThrows(ApiException.class,
                () -> orderService.checkout(client, request));

        // Assert
        assertThat(error.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void OrderService_FullFlow_MovesOrderFromPendingToDeliveredAndFreesDeliveryPerson() {
        // Arrange: un pedido recién creado
        Order created = orderService.checkout(client, buildRequest(tortilla.getId(), 1, null));
        Integer orderId = created.getId();

        // Act + Assert paso a paso: PENDING -> CONFIRMED
        Order confirmed = orderService.confirmOrder(orderId);
        assertThat(confirmed.getStatus()).isEqualTo(OrderStatus.CONFIRMED);

        // CONFIRMED -> PREPARING, queda registrado el operador que cocina
        Order preparing = orderService.cookOrder(orderId, operator.getId());
        assertThat(preparing.getStatus()).isEqualTo(OrderStatus.PREPARING);
        assertThat(preparing.getOperator().getId()).isEqualTo(operator.getId());

        // PREPARING -> ON_THE_WAY: el domiciliario queda ocupado y ya no es asignable
        Order onTheWay = orderService.sendOrder(orderId, deliveryPerson.getId());
        assertThat(onTheWay.getStatus()).isEqualTo(OrderStatus.ON_THE_WAY);
        assertThat(onTheWay.getDeliveryPerson().getId()).isEqualTo(deliveryPerson.getId());
        assertThat(deliveryPerson.getAvailable()).isFalse();
        assertThat(deliveryPersonRepository.findByAvailableTrueAndActiveTrue())
                .extracting(DeliveryPerson::getId)
                .doesNotContain(deliveryPerson.getId());

        // ON_THE_WAY -> DELIVERED: se registra la entrega y el domiciliario se libera
        Order delivered = orderService.deliverOrder(orderId);
        assertThat(delivered.getStatus()).isEqualTo(OrderStatus.DELIVERED);
        assertThat(delivered.getDeliveredAt()).isNotNull();
        assertThat(deliveryPerson.getAvailable()).isTrue();
        assertThat(deliveryPersonRepository.findByAvailableTrueAndActiveTrue())
                .extracting(DeliveryPerson::getId)
                .contains(deliveryPerson.getId());
    }

    @Test
    void OrderService_CancelOrder_ByClientThatDoesNotOwnIt_ThrowsForbidden() {
        // Arrange: el pedido es de "client", pero intenta cancelarlo "otroClient"
        Order created = orderService.checkout(client, buildRequest(tortilla.getId(), 1, null));
        Integer orderId = created.getId();
        Integer otroClientId = otroClient.getId();

        // Act
        ApiException error = assertThrows(ApiException.class,
                () -> orderService.cancelOrder(orderId, otroClientId));

        // Assert: 403 y el pedido sigue PENDING
        assertThat(error.getStatus()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(orderService.getOrderById(orderId).getStatus()).isEqualTo(OrderStatus.PENDING);
    }
}