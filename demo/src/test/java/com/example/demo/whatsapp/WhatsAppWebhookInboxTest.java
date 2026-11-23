package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.PlatformTransactionManager;
import jakarta.persistence.EntityManagerFactory;

@SpringJUnitConfig(WhatsAppWebhookInboxTest.Config.class)
class WhatsAppWebhookInboxTest {
    @Configuration
    static class Config {
        @Bean DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }
        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan("com.example.demo.whatsapp");
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop"));
            return factory;
        }
        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory factory) {
            return new JpaTransactionManager(factory);
        }
        @Bean WhatsAppWebhookInbox inbox(EntityManagerFactory factory, PlatformTransactionManager manager) {
            return new WhatsAppWebhookInbox(factory, manager);
        }
    }
    @Autowired WhatsAppWebhookInbox inbox;
    @Autowired EntityManagerFactory factory;

    @Test
    void repeatedConcurrentDeliveriesAreStoredOnceAndReadableFromNewEntityManager() throws Exception {
        byte[] body = "{\"message\":\"prueba de recepción\"}".getBytes(StandardCharsets.UTF_8);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> receive = () -> { inbox.receive(body); return null; };
            for (var result : executor.invokeAll(List.of(receive, receive))) result.get();
        }
        inbox.receive(body);
        try (var em = factory.createEntityManager()) {
            assertThat(em.createQuery("select count(e) from WhatsAppWebhookEvent e", Long.class).getSingleResult()).isEqualTo(1);
            assertThat(em.createQuery("select e.payload from WhatsAppWebhookEvent e", String.class).getSingleResult())
                    .isEqualTo(new String(body, StandardCharsets.UTF_8));
        }
    }
}
