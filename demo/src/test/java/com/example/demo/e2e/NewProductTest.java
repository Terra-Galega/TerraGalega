package com.example.demo.e2e;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

import io.github.bonigarcia.wdm.WebDriverManager;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class NewProductTest {

    private static final String FRONT = "http://localhost:4200";
    private static final String PRODUCT_NAME = "Limonada de Cereza";

    private ChromeOptions options;

    // Navegador del admin
    private WebDriver driver;
    private WebDriverWait wait;

    // Segundo navegador 
    private WebDriver menuDriver;
    private WebDriverWait menuWait;

    @BeforeEach
    public void setUp() {
        WebDriverManager.chromedriver().setup();
        options = new ChromeOptions();
        options.addArguments("--disable-extensions");
        options.addArguments("--start-maximized");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) driver.quit();
        if (menuDriver != null) menuDriver.quit();
    }

    // Espera a que el elemento exista en el navegador del admin y lo devuelve
    private WebElement waitForElement(By locator) {
        wait.until(ExpectedConditions.presenceOfElementLocated(locator));
        return driver.findElement(locator);
    }

    // Borra el 0 inicial antes de escribir el precio
    private void typePrice(String fieldId, String value) {
        WebElement priceField = driver.findElement(By.id(fieldId));
        priceField.sendKeys(Keys.BACK_SPACE);
        priceField.sendKeys(value);
    }

    // Recarga el menú en el navegador del visitante y abre el detalle del producto nuevo
    private void openDetailInMenu() {
        menuDriver.get(FRONT + "/menu");
        menuWait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("menu-product-name")));

        boolean found = false;
        for (WebElement title : menuDriver.findElements(By.className("menu-product-name"))) {
            if (title.getText().equals(PRODUCT_NAME)) {
                title.click();
                found = true;
                break;
            }
        }
        // Se espera que el producto nuevo aparezca en el menú
        assertTrue(found, "The product does not appear in the menu");
    }

    // Devuelve los nombres de los adicionales que muestra el detalle
    private List<String> readAddons() {
        menuWait.until(ExpectedConditions.presenceOfElementLocated(
                By.className("addon-item")));

        List<String> names = new ArrayList<>();
        for (WebElement nameElement : menuDriver.findElements(By.className("addon-item-name"))) {
            names.add(nameElement.getText());
        }
        return names;
    }

    @Test
    public void adminCreatesProductWithAddonsAndVerifiesItInMenu() {
        // Iniciar sesión como admin
        driver.get(FRONT + "/");
        waitForElement(By.id("nav-login")).click();

        // Contraseña incorrecta
        waitForElement(By.id("login-email")).sendKeys("admin@terra.com");
        WebElement password = driver.findElement(By.id("login-password"));
        password.sendKeys("admin123x");
        driver.findElement(By.id("btn-login")).click();

        WebElement error = waitForElement(By.id("login-error"));
        // Se espera el mensaje de error de credenciales incorrectas
        assertEquals("Correo o contraseña incorrectos.", error.getText());

        // Quitar la x e iniciar sesión con la contraseña correcta
        password.sendKeys(Keys.BACK_SPACE);
        driver.findElement(By.id("btn-login")).click();

        // Ir a productos
        waitForElement(By.id("tab-products")).click();

        // Crear el producto en la categoría "Bebidas"
        waitForElement(By.id("btn-add")).click();
        waitForElement(By.id("product-name")).sendKeys(PRODUCT_NAME);
        typePrice("product-price", "12000");
        driver.findElement(By.id("product-description")).sendKeys("Limonada natural con cereza.");
        driver.findElement(By.id("product-image")).sendKeys("https://example.com/limonada.jpg");

        waitForElement(By.className("category-option"));
        for (WebElement category : driver.findElements(By.className("category-option"))) {
            if (category.getText().equals("Bebidas")) {
                category.click();
            }
        }
        driver.findElement(By.id("btn-save-product")).click();

        // El modal se cierra solo cuando el servidor guardó el producto
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("product-name")));

        // El menú con los 2 adicionales de la categoría
        menuDriver = new ChromeDriver(options);
        menuWait = new WebDriverWait(menuDriver, Duration.ofSeconds(10));
        openDetailInMenu();

        List<String> addons = readAddons();
        // Se espera que el producto tenga adicionales: Hielo extra y Rodaja de naranja
        assertEquals(2, addons.size());
        assertTrue(addons.contains("Hielo extra"));
        assertTrue(addons.contains("Rodaja de naranja"));

        // Nuevo adicional para la categoría "Bebidas"
        waitForElement(By.id("tab-addons")).click();
        waitForElement(By.id("btn-add")).click();
        waitForElement(By.id("addon-name")).sendKeys("Menta fresca");
        typePrice("addon-price", "1000");
        driver.findElement(By.id("addon-description")).sendKeys("Hojas de menta fresca.");

        waitForElement(By.className("addon-category"));
        for (WebElement option : driver.findElements(By.className("check-option"))) {
            if (option.getText().equals("Bebidas")) {
                option.click();
            }
        }
        driver.findElement(By.id("btn-save-addon")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.id("addon-name")));

        openDetailInMenu();

        addons = readAddons();
        // Se espera que ahora haya 3 adicionales
        assertEquals(3, addons.size());
        assertTrue(addons.contains("Hielo extra"));
        assertTrue(addons.contains("Rodaja de naranja"));
        assertTrue(addons.contains("Menta fresca"));
    }
}
