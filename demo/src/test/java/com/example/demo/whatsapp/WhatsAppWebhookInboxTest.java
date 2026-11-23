package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.List;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import java.time.Instant;
import org.springframework.transaction.support.TransactionTemplate;
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
        @Bean WhatsAppReplySettings settings() { return new WhatsAppReplySettings(true, "fake-token", "123456", "v25.0"); }
        @Bean WhatsAppMessageClient sender() { return mock(WhatsAppMessageClient.class); }
        @Bean WhatsAppReplyService replies(EntityManagerFactory factory, PlatformTransactionManager manager,
                WhatsAppReplySettings settings, WhatsAppMessageClient sender) {
            return new WhatsAppReplyService(settings, sender, new ObjectMapper(), factory, manager);
        }
        @Bean WhatsAppWebhookInbox inbox(EntityManagerFactory factory, PlatformTransactionManager manager, WhatsAppReplyService replies) {
            return new WhatsAppWebhookInbox(factory, manager, replies);
        }
    }
    @Autowired WhatsAppWebhookInbox inbox;
    @Autowired EntityManagerFactory factory;
    @Autowired WhatsAppReplyService replies;
    @Autowired WhatsAppMessageClient sender;
    @Autowired PlatformTransactionManager manager;

    @BeforeEach
    void clean() {
        reset(sender);
        var em = org.springframework.orm.jpa.SharedEntityManagerCreator.createSharedEntityManager(factory);
        new TransactionTemplate(manager).executeWithoutResult(tx -> {
            em.createQuery("delete from WhatsAppReply").executeUpdate();
            em.createQuery("delete from WhatsAppWebhookEvent").executeUpdate();
        });
    }

    private byte[] event(String messageId, long timestamp, String phoneId) {
        return ("{\"object\":\"whatsapp_business_account\",\"entry\":[{\"changes\":[{\"field\":\"messages\",\"value\":{"
                + "\"metadata\":{\"phone_number_id\":\"" + phoneId + "\"},\"messages\":[{\"id\":\"" + messageId
                + "\",\"timestamp\":\"" + timestamp + "\",\"from\":\"573001234567\",\"type\":\"text\",\"text\":{\"body\":\"Hola\"}}]}}]}]}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private String state(String id) {
        try (var em = factory.createEntityManager()) { return em.find(WhatsAppReply.class, id).state; }
    }

    @Test
    void overlappingBatchesQueueOnlyOneReplyPerMessageAndDoNotReplyToStatuses() {
        byte[] body = event("wamid.test", Instant.now().getEpochSecond(), "123456");
        inbox.receive(body);
        inbox.receive((new String(body, StandardCharsets.UTF_8) + " ").getBytes(StandardCharsets.UTF_8));
        inbox.receive("{\"object\":\"whatsapp_business_account\",\"entry\":[{\"changes\":[{\"field\":\"messages\",\"value\":{\"metadata\":{\"phone_number_id\":\"123456\"},\"statuses\":[]}}]}]}".getBytes(StandardCharsets.UTF_8));
        when(sender.send("573001234567")).thenReturn("wamid.outbound");
        replies.dispatch();
        replies.dispatch();
        verify(sender, times(1)).send("573001234567");
        assertThat(state("wamid.test")).isEqualTo("ACCEPTED");
    }

    @Test
    void oldExamplesAndOtherPhoneNumbersNeverTriggerSending() {
        inbox.receive(event("old", 1504902988, "123456"));
        inbox.receive(event("wrong-phone", Instant.now().getEpochSecond(), "987654"));
        replies.dispatch();
        verifyNoInteractions(sender);
    }

    @Test
    void timeoutIsNotBlindlyRetried() {
        inbox.receive(event("timeout", Instant.now().getEpochSecond(), "123456"));
        when(sender.send(any())).thenThrow(new org.springframework.web.client.ResourceAccessException("timeout"));
        replies.dispatch();
        replies.dispatch();
        assertThat(state("timeout")).isEqualTo("UNKNOWN");
        verify(sender, times(1)).send(any());
    }

    @Test
    void rateLimitedReplyIsPersistedForRetry() {
        inbox.receive(event("limited", Instant.now().getEpochSecond(), "123456"));
        when(sender.send(any())).thenThrow(new org.springframework.web.client.HttpClientErrorException(org.springframework.http.HttpStatus.TOO_MANY_REQUESTS));
        replies.dispatch();
        assertThat(state("limited")).isEqualTo("PENDING");
        replies.dispatch();
        verify(sender, times(1)).send(any());
    }

    @Test
    void simultaneousWorkersDoNotSendTheSameReplyTwice() throws Exception {
        inbox.receive(event("concurrent", Instant.now().getEpochSecond(), "123456"));
        when(sender.send(any())).thenReturn("wamid.result");
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Void> send = () -> { replies.dispatch(); return null; };
            for (var result : executor.invokeAll(List.of(send, send))) result.get();
        }
        verify(sender, times(1)).send(any());
        assertThat(state("concurrent")).isEqualTo("ACCEPTED");
    }

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
