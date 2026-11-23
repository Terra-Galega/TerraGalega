package com.example.demo.whatsapp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.demo.security.CustomUserDetailsService;
import com.example.demo.security.JwtAccessDeniedHandler;
import com.example.demo.security.JwtAuthEntryPoint;
import com.example.demo.security.JwtGenerator;
import com.example.demo.security.SecurityConfig;

@WebMvcTest(WhatsAppWebhookController.class)
@Import({SecurityConfig.class, WhatsAppWebhookControllerTest.Config.class})
class WhatsAppWebhookControllerTest {
    @TestConfiguration
    static class Config {
        @Bean
        WhatsAppWebhookSecurity webhookSecurity() {
            return new WhatsAppWebhookSecurity("test-verify-token", "test-app-secret");
        }
    }

    @Autowired MockMvc mvc;
    @MockitoBean WhatsAppWebhookInbox inbox;
    @MockitoBean JwtGenerator jwtGenerator;
    @MockitoBean CustomUserDetailsService userDetailsService;
    @MockitoBean JwtAuthEntryPoint entryPoint;
    @MockitoBean JwtAccessDeniedHandler accessDeniedHandler;

    private static final String EVENT = "{\"object\":\"whatsapp_business_account\",\"entry\":[{\"id\":\"test\"}]}";

    static String signature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-app-secret".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return "sha256=" + HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void verifiesChallengeWithoutJwt() throws Exception {
        mvc.perform(get("/webhooks/whatsapp").param("hub.mode", "subscribe")
                .param("hub.verify_token", "test-verify-token").param("hub.challenge", "123456"))
                .andExpect(status().isOk()).andExpect(content().string("123456"))
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN));
    }

    @Test
    void rejectsWrongTokenOrMissingChallenge() throws Exception {
        mvc.perform(get("/webhooks/whatsapp").param("hub.mode", "subscribe")
                .param("hub.verify_token", "wrong").param("hub.challenge", "123"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/webhooks/whatsapp").param("hub.mode", "subscribe")
                .param("hub.verify_token", "test-verify-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void acceptsSignedEventWithoutJwtAndStoresOriginalBody() throws Exception {
        mvc.perform(post("/webhooks/whatsapp").contentType(MediaType.APPLICATION_JSON).content(EVENT)
                .header("X-Hub-Signature-256", signature(EVENT)))
                .andExpect(status().isOk()).andExpect(content().string("EVENT_RECEIVED"));
        verify(inbox).receive(EVENT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void rejectsMissingMalformedAndTamperedSignatures() throws Exception {
        for (String header : new String[]{"", "sha256=oops", signature(EVENT + " ")}) {
            mvc.perform(post("/webhooks/whatsapp").contentType(MediaType.APPLICATION_JSON).content(EVENT)
                    .header("X-Hub-Signature-256", header)).andExpect(status().isForbidden());
        }
        verifyNoInteractions(inbox);
    }

    @Test
    void rejectsSignedMalformedJsonAndOtherObjects() throws Exception {
        for (String body : new String[]{"{", "null", "{\"object\":\"page\",\"entry\":[]}"}) {
            mvc.perform(post("/webhooks/whatsapp").contentType(MediaType.APPLICATION_JSON).content(body)
                    .header("X-Hub-Signature-256", signature(body))).andExpect(status().isBadRequest());
        }
        verifyNoInteractions(inbox);
    }

    @Test
    void storageFailureDoesNotAcknowledgeDelivery() throws Exception {
        doThrow(new IllegalStateException("Storage unavailable")).when(inbox).receive(any());
        mvc.perform(post("/webhooks/whatsapp").contentType(MediaType.APPLICATION_JSON).content(EVENT)
                .header("X-Hub-Signature-256", signature(EVENT))).andExpect(status().is5xxServerError());
    }

    @Test
    void missingConfigurationFailsClosed() {
        var security = new WhatsAppWebhookSecurity("", "");
        org.assertj.core.api.Assertions.assertThat(security.configured()).isFalse();
        org.assertj.core.api.Assertions.assertThat(security.verifyToken("")).isFalse();
        org.assertj.core.api.Assertions.assertThat(security.verifySignature(new byte[0], null)).isFalse();
    }
}
