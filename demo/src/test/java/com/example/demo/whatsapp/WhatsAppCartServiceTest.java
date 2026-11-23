package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.context.annotation.*;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.*;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.demo.entities.*;
import com.example.demo.entities.Role;
import com.example.demo.errors.ApiException;
import com.example.demo.dto.CheckoutItemDTO;
import com.example.demo.movil.entity.WhatsAppCartAction;
import com.example.demo.movil.repository.CartLineRepository;
import com.example.demo.movil.service.CartService;
import com.example.demo.repository.ProductRepository;
import com.example.demo.service.ClientService;
import tools.jackson.databind.ObjectMapper;

@SpringJUnitConfig(WhatsAppCartServiceTest.Config.class)
class WhatsAppCartServiceTest {
    @Configuration @EnableTransactionManagement
    @Import({PrimaryRepositories.class, CartRepositories.class})
    static class Config {
        @Bean @Primary DataSource dataSource() { return db(); }
        @Bean DataSource postgresDataSource() { return db(); }
        @Bean @Primary LocalContainerEntityManagerFactoryBean entityManagerFactory(@Qualifier("dataSource") DataSource ds) {
            return emf(ds, "h2", "com.example.demo.entities", "com.example.demo.whatsapp");
        }
        @Bean LocalContainerEntityManagerFactoryBean postgresEntityManagerFactory(@Qualifier("postgresDataSource") DataSource ds) {
            return emf(ds, "postgres", "com.example.demo.movil.entity");
        }
        @Bean @Primary PlatformTransactionManager transactionManager(@Qualifier("entityManagerFactory") EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean PlatformTransactionManager postgresTransactionManager(@Qualifier("postgresEntityManagerFactory") EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean ClientService clientService() {
            var service = mock(ClientService.class);
            when(service.getClientById(anyInt())).thenAnswer(call -> Client.builder().id(call.getArgument(0)).build());
            return service;
        }
        @Bean CartService cart(@Qualifier("postgresTransactionManager") PlatformTransactionManager manager) { return new CartService(manager); }
        @Bean ObjectMapper mapper() { return new ObjectMapper(); }
        @Bean WhatsAppAccountService accounts(@Qualifier("entityManagerFactory") EntityManagerFactory factory,
                @Qualifier("transactionManager") PlatformTransactionManager manager) { return new WhatsAppAccountService(factory, manager, "https://terra.example"); }
        @Bean WhatsAppCartService whatsappCart(WhatsAppAccountService accounts, CartService cart, ProductRepository products,
                @Qualifier("postgresEntityManagerFactory") EntityManagerFactory factory,
                @Qualifier("postgresTransactionManager") PlatformTransactionManager manager, ObjectMapper mapper) {
            return new WhatsAppCartService(accounts, cart, products, factory, manager, mapper);
        }
        static DataSource db() { return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build(); }
        static LocalContainerEntityManagerFactoryBean emf(DataSource ds, String unit, String... packages) {
            var factory = new LocalContainerEntityManagerFactoryBean(); factory.setDataSource(ds); factory.setPersistenceUnitName(unit);
            factory.setPackagesToScan(packages); factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop")); return factory;
        }
    }
    @Configuration @EnableJpaRepositories(basePackageClasses = ProductRepository.class, entityManagerFactoryRef = "entityManagerFactory", transactionManagerRef = "transactionManager")
    static class PrimaryRepositories { }
    @Configuration @EnableJpaRepositories(basePackageClasses = CartLineRepository.class, entityManagerFactoryRef = "postgresEntityManagerFactory", transactionManagerRef = "postgresTransactionManager")
    static class CartRepositories { }

    @Autowired WhatsAppCartService service;
    @Autowired WhatsAppAccountService accounts;
    @Autowired CartService cart;
    @Autowired @Qualifier("entityManagerFactory") EntityManagerFactory factory;
    @Autowired @Qualifier("postgresEntityManagerFactory") EntityManagerFactory pgFactory;
    @Autowired @Qualifier("transactionManager") PlatformTransactionManager manager;
    @Autowired @Qualifier("postgresTransactionManager") PlatformTransactionManager pgManager;
    Integer first, second, productId;
    static final String PHONE = "573001234567";
    WhatsAppAccountService.Account account;

    @BeforeEach void setup() {
        var pg = SharedEntityManagerCreator.createSharedEntityManager(pgFactory);
        new TransactionTemplate(pgManager).executeWithoutResult(tx -> {
            pg.createQuery("delete from WhatsAppCartAction").executeUpdate(); pg.createQuery("delete from CartLine").executeUpdate();
        });
        var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            em.createQuery("delete from WhatsAppAccountLink").executeUpdate();
            var a = Client.builder().name("Ana").lastName("A").role(Role.CLIENT).email(UUID.randomUUID() + "@example.com").build();
            var b = Client.builder().name("Luis").lastName("B").role(Role.CLIENT).email(UUID.randomUUID() + "@example.com").build();
            var category = Category.builder().name(UUID.randomUUID().toString()).build(); em.persist(category);
            var product = Product.builder().name("Pulpo gallego").price(45000d).category(category).active(true).popular(false).build();
            em.persist(a); em.persist(b); em.persist(product); em.flush(); first = a.getId(); second = b.getId(); productId = product.getId();
        });
        link(first);
    }

    void link(Integer clientId) {
        String token = accounts.createReply(PHONE).split("#token=")[1].split("\\s")[0]; accounts.confirm(clientId, token);
        account = new TransactionTemplate(manager).execute(tx -> accounts.snapshot(SharedEntityManagerCreator.createSharedEntityManager(factory), PHONE));
    }
    WhatsAppCartService.Context context(String id) { return new WhatsAppCartService.Context(id, PHONE, account); }
    Map<String, Object> add(int quantity) { return Map.of("accion", "agregar", "productoId", productId, "cantidad", quantity); }
    void webAdd(int quantity) {
        var item = new CheckoutItemDTO(); item.setProductId(productId); item.setQuantity(quantity); item.setAddOnIds(List.of()); cart.addItem(first, item);
    }

    @Test void proposalDoesNotMutateAndDoubleConfirmationChangesSharedCartOnce() {
        var pending = service.prepare(context("add-two"), add(2));
        assertThat(pending.text()).contains("Agregar 2", "Pulpo", "Confirm");
        assertThat(cart.getCart(first)).isEmpty();
        assertThat(service.prepare(context("add-two"), add(2)).actionId()).isEqualTo(pending.actionId());
        assertThat(service.decide(context("confirm-1"), pending.actionId(), true).text()).contains("Cambio confirmado", "90.000");
        assertThat(service.decide(context("confirm-2"), pending.actionId(), true).text()).contains("ya fue aplicado");
        assertThat(cart.getCart(first)).singleElement().satisfies(line -> assertThat(line.quantity()).isEqualTo(2));
        assertThat(cart.getCart(second)).isEmpty();
    }

    @Test void setQuantityIsTotalAndRemoveKeepsOtherAccountsIntact() {
        webAdd(2);
        String line = cart.getCart(first).getFirst().id();
        var change = service.prepare(context("set-three"), Map.of("accion", "cantidad", "lineaId", line, "cantidad", 3));
        service.decide(context("confirm-three"), change.actionId(), true);
        assertThat(cart.getCart(first).getFirst().quantity()).isEqualTo(3);
        var remove = service.prepare(context("remove"), Map.of("accion", "quitar", "lineaId", line));
        service.decide(context("confirm-remove"), remove.actionId(), true);
        assertThat(cart.getCart(first)).isEmpty();
    }

    @Test void cancellingReplacingAndExpirationNeverApplyChanges() {
        var old = service.prepare(context("old"), add(1));
        var fresh = service.prepare(context("fresh"), add(2));
        assertThat(service.decide(context("old-confirm"), old.actionId(), true).text()).contains("cancelado");
        assertThat(service.decide(context("cancel"), fresh.actionId(), false).text()).contains("cancelado");
        var expired = service.prepare(context("expire"), add(2));
        var pg = SharedEntityManagerCreator.createSharedEntityManager(pgFactory);
        new TransactionTemplate(pgManager).executeWithoutResult(tx -> pg.find(WhatsAppCartAction.class, expired.actionId()).setExpiresAt(Instant.now().minusSeconds(1)));
        assertThat(service.decide(context("late"), expired.actionId(), true).text()).contains("venció");
        assertThat(cart.getCart(first)).isEmpty();
    }

    @Test void webChangesInvalidateProposalRatherThanOverwritingThem() {
        webAdd(1);
        String line = cart.getCart(first).getFirst().id();
        var pending = service.prepare(context("propose-three"), Map.of("accion", "cantidad", "lineaId", line, "cantidad", 3));
        cart.setQuantity(first, line, 5);
        assertThat(service.decide(context("confirm-after-web"), pending.actionId(), true).text()).contains("cambiaron", "No apliqué");
        assertThat(cart.getCart(first).getFirst().quantity()).isEqualTo(5);
    }

    @Test void unlinkAndRelinkCannotReuseOldProposalsOrReadAnotherAccount() {
        var stale = context("old-client"); var pending = service.prepare(stale, add(1)); accounts.unlink(first);
        assertThatThrownBy(() -> service.summary(stale)).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.decide(stale, pending.actionId(), true)).isInstanceOf(ApiException.class);
        link(second);
        assertThatThrownBy(() -> service.decide(context("new-owner"), pending.actionId(), true)).isInstanceOf(ApiException.class);
        assertThat(cart.getCart(first)).isEmpty(); assertThat(cart.getCart(second)).isEmpty();
    }

    @Test void arbitraryClientIdsAndUnlinkedPhonesAreRejected() {
        assertThatThrownBy(() -> service.summary(new WhatsAppCartService.Context("x", PHONE, null))).isInstanceOf(ApiException.class);
        var forged = new WhatsAppAccountService.Account(second, account.linkedAt());
        assertThatThrownBy(() -> service.prepare(new WhatsAppCartService.Context("x", PHONE, forged), add(1))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.prepare(new WhatsAppCartService.Context("x", "573009999999", account), add(1))).isInstanceOf(ApiException.class);
    }

    @Test void invalidQuantitiesProductsAndLinesDoNotCreateProposals() {
        for (Object quantity : List.of(0, -1, 100, 1.5, "NaN")) {
            assertThatThrownBy(() -> service.prepare(context("bad-" + quantity), Map.of("accion", "agregar", "productoId", productId, "cantidad", quantity))).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> service.prepare(context("line"), Map.of("accion", "quitar", "lineaId", "not-mine"))).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> service.prepare(context("missing"), Map.of("accion", "agregar", "productoId", -1, "cantidad", 1))).isInstanceOf(ApiException.class);
        assertThat(cart.getCart(first)).isEmpty();
    }

    @Test void changedOrInactiveProductCannotBeAddedAfterConfirmation() {
        var pending = service.prepare(context("price"), add(1));
        var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        new TransactionTemplate(manager).executeWithoutResult(tx -> em.find(Product.class, productId).setPrice(49000d));
        assertThat(service.decide(context("price-confirm"), pending.actionId(), true).text()).contains("cambiaron");
        var active = service.prepare(context("inactive"), add(1));
        new TransactionTemplate(manager).executeWithoutResult(tx -> em.find(Product.class, productId).setActive(false));
        assertThatThrownBy(() -> service.decide(context("inactive-confirm"), active.actionId(), true)).isInstanceOf(ApiException.class);
        assertThat(cart.getCart(first)).isEmpty();
    }

    @Test void duplicateConfirmationsInParallelAreSerialized() throws Exception {
        var pending = service.prepare(context("parallel"), add(2));
        try (var pool = Executors.newFixedThreadPool(2)) {
            for (var result : pool.invokeAll(List.<Callable<WhatsAppCartService.Reply>>of(
                    () -> service.decide(context("a"), pending.actionId(), true), () -> service.decide(context("b"), pending.actionId(), true)))) result.get();
        }
        assertThat(cart.getCart(first).getFirst().quantity()).isEqualTo(2);
    }

    @Test void cartAndReceiptRollbackTogetherInSecondaryDatabase() {
        var pending = service.prepare(context("atomic"), add(2));
        assertThatThrownBy(() -> new TransactionTemplate(pgManager).executeWithoutResult(tx -> {
            service.decide(context("apply"), pending.actionId(), true);
            throw new IllegalStateException("simulated failure before PG commit");
        })).isInstanceOf(IllegalStateException.class);
        assertThat(cart.getCart(first)).isEmpty();
        assertThat(service.decide(context("retry"), pending.actionId(), true).text()).contains("Cambio confirmado");
        assertThat(cart.getCart(first).getFirst().quantity()).isEqualTo(2);
    }
}
