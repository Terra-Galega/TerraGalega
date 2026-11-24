package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.*;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import javax.sql.DataSource;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.*;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import com.example.demo.entities.Client;
import com.example.demo.entities.Role;
import com.example.demo.errors.ApiException;

@SpringJUnitConfig(WhatsAppAccountServiceTest.Config.class)
class WhatsAppAccountServiceTest {
    @Configuration
    static class Config {
        @Bean DataSource dataSource() { return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build(); }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource ds) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(ds);
            factory.setPackagesToScan("com.example.demo.entities", "com.example.demo.whatsapp");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) { return new JpaTransactionManager(factory); }
        @Bean WhatsAppAccountService accounts(EntityManagerFactory factory, PlatformTransactionManager manager) {
            return new WhatsAppAccountService(factory, manager, "https://terra.example/");
        }
    }
    @Autowired WhatsAppAccountService accounts;
    @Autowired EntityManagerFactory factory;
    @Autowired PlatformTransactionManager manager;
    Integer first, second;
    static final String PHONE = "573001234567";
    static final String OTHER = "573009876543";

    @BeforeEach
    void clients() {
        var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            em.createQuery("delete from WhatsAppAccountLink").executeUpdate();
            var a = Client.builder().name("Ana").lastName("Prueba").email(UUID.randomUUID() + "@example.com").role(Role.CLIENT).build();
            var b = Client.builder().name("Luis").lastName("Prueba").email(UUID.randomUUID() + "@example.com").role(Role.CLIENT).build();
            em.persist(a); em.persist(b); em.flush(); first = a.getId(); second = b.getId();
        });
    }

    String issue(String phone) {
        String reply = accounts.createReply(phone);
        assertThat(reply).contains("https://terra.example/whatsapp/link#token=", "15 minutos");
        return reply.split("#token=")[1].split("\\s")[0];
    }

    @Test void explicitConfirmationConsumesTokenAndKeepsContactPhoneIndependent() {
        String token = issue(PHONE);
        assertThat(token).hasSize(43);
        try (var em = factory.createEntityManager()) {
            assertThat(em.find(WhatsAppAccountLink.class, PHONE).tokenHash).hasSize(64).doesNotContain(token);
        }
        assertThat(accounts.preview(token).maskedPhone()).isEqualTo("+57 •••••• 4567");
        assertThat(accounts.status(first).linked()).isFalse();
        assertThat(accounts.confirm(first, token).phone()).isEqualTo(PHONE);
        assertThat(accounts.status(second).linked()).isFalse();
        assertThatThrownBy(() -> accounts.confirm(second, token)).isInstanceOf(ApiException.class);
        assertThat(accounts.createReply(PHONE)).contains("ya está vinculado").doesNotContain("#token=");
        try (var em = factory.createEntityManager()) {
            assertThat(em.createQuery("select c.phone from Client c where c.id = :id", String.class).setParameter("id", first).getSingleResult()).isNull();
        }
    }

    @Test void newRequestInvalidatesPreviousLinkAndExpiredOrMalformedTokensFail() {
        String old = issue(PHONE), fresh = issue(PHONE);
        assertThat(fresh).isNotEqualTo(old);
        assertThatThrownBy(() -> accounts.preview(old)).isInstanceOf(ApiException.class);
        var em = SharedEntityManagerCreator.createSharedEntityManager(factory);
        new TransactionTemplate(manager).executeWithoutResult(tx -> em.find(WhatsAppAccountLink.class, PHONE).expiresAt = Instant.now().minusSeconds(1));
        assertThatThrownBy(() -> accounts.confirm(first, fresh)).isInstanceOf(ApiException.class);
        for (String invalid : new String[]{"", "invalid", "a".repeat(43)}) {
            assertThatThrownBy(() -> accounts.preview(invalid)).isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> accounts.preview(null)).isInstanceOf(ApiException.class);
    }

    @Test void oneNumberPerAccountAndOnlyOwnerCanUnlinkWithoutRevivingOldLinks() {
        String old = issue(PHONE);
        accounts.confirm(first, old);
        accounts.unlink(second);
        assertThat(accounts.status(first).linked()).isTrue();
        String other = issue(OTHER);
        assertThatThrownBy(() -> accounts.confirm(first, other)).isInstanceOf(ApiException.class);
        assertThat(accounts.status(first).phone()).isEqualTo(PHONE);
        accounts.unlink(first);
        assertThat(accounts.status(first).linked()).isFalse();
        assertThatThrownBy(() -> accounts.confirm(second, old)).isInstanceOf(ApiException.class);
        accounts.confirm(second, issue(PHONE));
        assertThat(accounts.status(second).phone()).isEqualTo(PHONE);
    }

    @Test void concurrentConsumersCannotLinkOneTokenToTwoAccounts() throws Exception {
        String token = issue(PHONE);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var outcomes = pool.invokeAll(List.<Callable<Boolean>>of(() -> consume(first, token), () -> consume(second, token)));
            assertThat(List.of(outcomes.get(0).get(), outcomes.get(1).get())).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test void concurrentDifferentNumbersCannotOverwriteOneAccount() throws Exception {
        String a = issue(PHONE), b = issue(OTHER);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var outcomes = pool.invokeAll(List.<Callable<Boolean>>of(() -> consume(first, a), () -> consume(first, b)));
            assertThat(List.of(outcomes.get(0).get(), outcomes.get(1).get())).containsExactlyInAnyOrder(true, false);
        }
    }

    @Test void deletedAccountAndInvalidWebsiteCannotProduceNewAssociations() {
        String token = issue(PHONE);
        assertThatThrownBy(() -> accounts.confirm(-1, token)).isInstanceOf(ApiException.class);
        for (String website : new String[]{"", "http://localhost:4200", "https://terra.example/#unsafe", "https://terra.example/?unsafe"}) {
            assertThat(new WhatsAppAccountService(factory, manager, website).createReply(OTHER)).doesNotContain("#token=");
        }
    }

    private boolean consume(Integer clientId, String token) {
        try { accounts.confirm(clientId, token); return true; }
        catch (ApiException unavailable) { return false; }
    }
}
