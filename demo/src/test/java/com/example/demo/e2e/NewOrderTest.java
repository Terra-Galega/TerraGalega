package com.example.demo.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.example.demo.repository.AddressRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import com.example.demo.entities.AddOn;
import com.example.demo.entities.Order;
import com.example.demo.entities.Product;
import com.example.demo.repository.AddOnRepository;
import com.example.demo.repository.ClientRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.ProductRepository;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NewOrderTest {

    private static final String FRONT = "http://localhost:4200";

    private static final String PRODUCT_1 = "Tortilla Española";
    private static final String PRODUCT_2 = "Patatas Bravas";

    // Espera 25s por refresh cada 10s
    private static final Duration POLLING_TIMEOUT = Duration.ofSeconds(25);

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private AddOnRepository addOnRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private ClientRepository clientRepository;
    @Autowired
    private AddressRepository addressRepository;

    private ChromeOptions options;
    private WebDriver clientDriver;
    private WebDriverWait clientWait;
    private WebDriver operatorDriver;
    private WebDriverWait operatorWait;

    // Lo que eligió el cliente: producto -> adicionales
    private final Map<String, List<String>> selectedAddons = new LinkedHashMap<>();

    // Se calculan con los precios de la base de datos
    private double expectedSubtotal = 0;
    private double expectedTotal = 0;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        options = new ChromeOptions();
        options.addArguments("--disable-extensions");
        options.addArguments("--start-maximized");
        options.addArguments("--disable-backgrounding-occluded-windows");
        options.addArguments("--disable-renderer-backgrounding");
        options.addArguments("--disable-background-timer-throttling");
        options.addArguments("--disable-features=CalculateNativeWinOcclusion");

        options.setExperimentalOption("prefs",
                Map.of("profile.password_manager_leak_detection", false));

        clientDriver = new ChromeDriver(options);
        clientWait = new WebDriverWait(clientDriver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (clientDriver != null)
            clientDriver.quit();
        if (operatorDriver != null)
            operatorDriver.quit();
    }

    private WebElement waitFor(WebDriver driver, WebDriverWait wait, By locator) {
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        return driver.findElement(locator);
    }

    private WebElement clientElement(By locator) {
        return waitFor(clientDriver, clientWait, locator);
    }

    private WebElement operatorElement(By locator) {
        return waitFor(operatorDriver, operatorWait, locator);
    }

    // "$62,000" -> 62000
    private double parseMoney(String text) {
        return Double.parseDouble(text.replaceAll("[^0-9]", ""));
    }

    private double productPrice(String name) {
        for (Product product : productRepository.findAll()) {
            if (product.getName().equals(name)) {
                return product.getPrice();
            }
        }
        throw new RuntimeException("Product not found: " + name);
    }

    private int productId(String name) {
        for (Product product : productRepository.findAll()) {
            if (product.getName().equals(name)) {
                return product.getId();
            }
        }
        throw new RuntimeException("Product not found: " + name);
    }

    private double addonPrice(String name) {
        for (AddOn addon : addOnRepository.findAll()) {
            if (addon.getName().equals(name)) {
                return addon.getPrice();
            }
        }
        throw new RuntimeException("Add-on not found: " + name);
    }

    private int latestOrderId() {
        int latestId = -1;
        for (Order order : orderRepository.findAll()) {
            if (order.getId() > latestId) {
                latestId = order.getId();
            }
        }
        if (latestId == -1) {
            throw new RuntimeException("No orders found");
        }
        return latestId;
    }

    // Tarjeta del pedido en el panel del operador
    private By operatorOrderCard(int orderId) {
        return By.id("operator-order-" + orderId);
    }

    // El botón conserva el id y solo cambia su texto: se espera a que aparezca la
    // etiqueta
    private WebElement operatorAction(int orderId, String label) {
        By button = By.id("operator-action-" + orderId);
        operatorWait.until(ExpectedConditions.textToBePresentInElementLocated(button, label));
        return operatorDriver.findElement(button);
    }

    // Espera, sin recargar, a que la página del cliente muestre el nuevo estado
    private void waitForClientStatus(int orderId, String statusLabel) {
        By currentStep = By.xpath("//*[@id='order-status-" + orderId + "']//li[@aria-current='step']");
        new WebDriverWait(clientDriver, POLLING_TIMEOUT)
                .until(ExpectedConditions.textToBePresentInElementLocated(currentStep, statusLabel));
    }

    // CLiente

    // Busca el producto en el menú, elige dos adicionales y lo agrega al carrito
    private void addProductToCart(String productName) {
        clientElement(By.id("nav-menu")).click();
        clientElement(By.xpath("//*[@id='search-input']//input")).sendKeys(productName);

        // Abre la tarjeta cuyo nombre coincide exactamente+
        clientWait.until(ExpectedConditions.elementToBeClickable(
                By.id("menu-product-" + productId(productName)))).click();

        // Elige los dos primeros adicionales y guarda sus nombres
        clientElement(By.className("addon-item"));
        List<WebElement> rows = clientDriver.findElements(By.className("addon-item"));
        List<WebElement> plusButtons = clientDriver
                .findElements(By.xpath("//button[@aria-label='Agregar uno']"));
        List<String> chosen = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            chosen.add(rows.get(i).findElement(By.className("addon-item-name")).getText());
            plusButtons.get(i).click();
        }

        clientElement(By.id("btn-add-to-order")).click();

        // El precio esperado sale de la base de datos (cantidad 1)
        double unitPrice = productPrice(productName);
        for (String addon : chosen) {
            unitPrice += addonPrice(addon);
        }
        selectedAddons.put(productName, chosen);
        expectedSubtotal += unitPrice;
    }

    private void clientCreatesAnOrderWithAddons() {
        // Iniciar sesión como cliente
        clientDriver.get(FRONT + "/");
        clientElement(By.id("nav-login")).click();
        clientElement(By.id("login-email")).sendKeys("juan.garcia@gmail.com");
        clientDriver.findElement(By.id("login-password")).sendKeys("cliente123");
        clientDriver.findElement(By.id("btn-login")).click();
        // Al iniciar sesión, el cliente llega a su perfil
        clientElement(By.id("profile-title"));

        addProductToCart(PRODUCT_1);
        addProductToCart(PRODUCT_2);

        // Abrir el carrito y revisar productos, adicionales y total
        clientElement(By.id("nav-cart")).click();

        for (String product : selectedAddons.keySet()) {
            int productId = productId(product);
            clientElement(By.id("cart-item-" + productId));

            String addonsLine = clientDriver
                    .findElement(By.id("cart-item-addons-" + productId))
                    .getText();

            for (String addon : selectedAddons.get(product)) {
                // Se espera que cada adicional elegido aparezca en la línea del producto
                assertTrue(addonsLine.contains(addon),
                        "The cart line of " + product + " does not show " + addon);
            }
        }

        double cartTotal = parseMoney(clientElement(By.id("cart-total")).getText());
        // Se espera que el total del carrito sea la suma de productos y adicionales
        assertEquals(expectedSubtotal, cartTotal);

        // Checkout (pasos 1 y 2), pagando contra entrega
        clientElement(By.id("btn-cart-checkout")).click();

        // Paso 1: la dirección ya viene del perfil; se elige contra entrega
        clientElement(By.id("payment-cash")).click();
        clientElement(By.id("btn-continue")).click();

        // Paso 2: revisar el resumen del pedido
        double fee = parseMoney(clientElement(By.id("summary-delivery-fee")).getText());
        double subtotal = parseMoney(clientDriver.findElement(By.id("summary-subtotal")).getText());
        double total = parseMoney(clientDriver.findElement(By.id("summary-total")).getText());

        // Se espera que el subtotal del resumen sea el de los productos elegidos
        assertEquals(expectedSubtotal, subtotal);
        // Se espera que el total sea subtotal + domicilio
        assertEquals(expectedSubtotal + fee, total);
        expectedTotal = total;

        clientElement(By.id("btn-confirm-order")).click();
        // Se espera el aviso de que el pedido se creó con éxito
        clientElement(By.id("order-success"));
    }

    // El historial está en "Mis pedidos" > "Pedidos pasados", no en el perfil
    private void clientVerifiesCompletedOrder(int orderId) {
        clientElement(By.id("nav-orders")).click();

        By pastCard = By.xpath("//*[@id='past-orders']//*[@id='order-" + orderId + "']");
        WebElement card = new WebDriverWait(clientDriver, POLLING_TIMEOUT)
                .until(ExpectedConditions.presenceOfElementLocated(pastCard));

        String text = card.getText();
        for (String product : selectedAddons.keySet()) {
            // Se espera que cada producto comprado aparezca en la tarjeta
            assertTrue(text.contains(product), "Missing product " + product);

            for (String addon : selectedAddons.get(product)) {
                // Se espera que cada adicional aparezca con cantidad x1
                assertTrue(text.contains("x1 " + addon), "Missing add-on " + addon);
            }
        }

        double total = parseMoney(card.findElement(By.id("order-total-" + orderId)).getText());
        // Se espera que el total del historial sea el que se pagó
        assertEquals(expectedTotal, total);

        // La dirección que propone el checkout es la predeterminada del cliente
        Integer clientId = clientRepository.findByEmail("juan.garcia@gmail.com").getId();
        String address = addressRepository
                .findByClientIdOrderByDefaultAddressDescIdAsc(clientId).get(0).getLabel();

        // Se espera que la tarjeta del pedido muestre esa dirección
        assertTrue(text.contains(address), "Se perdió la dirección de entrega: " + address);
    }

    // operador

    private void operatorProcessesOrder(int orderId) {
        operatorDriver = new ChromeDriver(options);
        operatorWait = new WebDriverWait(operatorDriver, Duration.ofSeconds(10));

        operatorDriver.get(FRONT + "/");
        operatorElement(By.id("nav-login")).click();
        operatorElement(By.id("login-email")).sendKeys("sofia.jimenez@terra.com");
        operatorDriver.findElement(By.id("login-password")).sendKeys("sofia123");
        operatorDriver.findElement(By.id("btn-login")).click();
        operatorElement(By.id("operator-title"));

        // Abrir el detalle del pedido nuevo
        operatorElement(By.id("operator-order-client-" + orderId)).click();
        operatorWait.until(ExpectedConditions.textToBe(By.id("detail-order-id"), "#" + orderId));

        String detail = operatorElement(By.id("detail-items")).getText();
        for (String product : selectedAddons.keySet()) {
            // Se espera que el detalle muestre cada producto que compró el cliente
            assertTrue(detail.contains(product), "Detail is missing " + product);
            for (String addon : selectedAddons.get(product)) {
                // Se espera que el detalle muestre cada adicional elegido
                assertTrue(detail.contains(addon), "Detail is missing " + addon);
            }
        }
        double detailTotal = parseMoney(operatorElement(By.id("detail-total")).getText());
        // Se espera que el total del detalle sea el que vio el cliente
        assertEquals(expectedTotal, detailTotal);

        operatorDriver.findElement(By.id("btn-close-detail")).click();
        operatorWait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("detail-order-id")));

        // Pendiente -> Confirmado
        operatorAction(orderId, "Confirmar").click();
        operatorAction(orderId, "Cocinar");
        waitForClientStatus(orderId, "Confirmado");

        // Confirmado -> Preparando
        operatorAction(orderId, "Cocinar").click();
        operatorAction(orderId, "Enviar");
        waitForClientStatus(orderId, "Preparando");

        // Preparando -> En camino, asignando un domiciliario
        operatorAction(orderId, "Enviar").click();
        List<WebElement> deliveryOptions = operatorElement(By.id("delivery-person-select"))
                .findElements(By.tagName("option"));
        WebElement deliveryPerson = deliveryOptions.get(1); // la opción 0 es el texto por defecto
        deliveryPerson.click();
        String deliveryPersonName = deliveryPerson.getText().split("·")[0].trim();
        operatorElement(By.id("btn-send-order")).click();

        operatorAction(orderId, "Completar");
        // Se espera que la tarjeta muestre el domiciliario asignado
        assertTrue(operatorDriver.findElement(operatorOrderCard(orderId)).getText().contains(deliveryPersonName),
                "The card does not show the assigned delivery person");
        waitForClientStatus(orderId, "En camino");

        // En camino -> Completado
        operatorAction(orderId, "Completar").click();
        // Se espera el aviso de pedido completado
        operatorElement(By.id("operator-toast-title"));
    }

    // ----------------------------------------------------------------- test

    @Test
    public void fullFlowOrder() {
        clientCreatesAnOrderWithAddons();

        // El pedido nuev
        int orderId = latestOrderId();
        waitForClientStatus(orderId, "Pendiente");

        // Se espera que el domicilio mostrado en pantalla sea el guardado en el backend
        Order order = orderRepository.findById(orderId).get();
        assertEquals(expectedTotal - expectedSubtotal, order.getDeliveryFee());

        operatorProcessesOrder(orderId);
        clientVerifiesCompletedOrder(orderId);
    }
}