package com.example.demo.whatsapp;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import com.example.demo.security.*;

@WebMvcTest({WhatsAppContactController.class, WhatsAppAccountController.class})
@Import(SecurityConfig.class)
class WhatsAppContactSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean(name = "entityManagerFactory") EntityManagerFactory factory;
    @MockitoBean(name = "transactionManager") PlatformTransactionManager manager;
    @MockitoBean JwtGenerator jwt;
    @MockitoBean CustomUserDetailsService users;
    @MockitoBean JwtAuthEntryPoint entryPoint;
    @MockitoBean JwtAccessDeniedHandler denied;
    @MockitoBean AuthenticatedUser authenticatedUser;
    @MockitoBean WhatsAppAccountService accounts;

    @BeforeEach
    void securityResponses() throws Exception {
        doAnswer(call -> { ((jakarta.servlet.http.HttpServletResponse)call.getArgument(1)).setStatus(401); return null; })
                .when(entryPoint).commence(any(), any(), any());
        doAnswer(call -> { ((jakarta.servlet.http.HttpServletResponse)call.getArgument(1)).setStatus(403); return null; })
                .when(denied).handle(any(), any(), any());
        when(jwt.extractUsername("client-token")).thenReturn("client");
        when(users.loadUserByUsername("client")).thenReturn(User.withUsername("client").password("unused").authorities("CLIENT").build());
        when(jwt.extractUsername("operator-token")).thenReturn("operator");
        when(users.loadUserByUsername("operator")).thenReturn(User.withUsername("operator").password("unused").authorities("OPERATOR").build());
    }

    @Test
    void whatsappLinkingRequiresCustomerSession() throws Exception {
        for (String authorization : new String[]{"", "Bearer operator-token"}) {
            int expected = authorization.isEmpty() ? 401 : 403;
            mvc.perform(get("/clients/me/whatsapp").header("Authorization", authorization)).andExpect(status().is(expected));
            mvc.perform(post("/clients/me/whatsapp/preview").header("Authorization", authorization)
                    .contentType("application/json").content("{\"token\":\"example\"}")).andExpect(status().is(expected));
            mvc.perform(post("/clients/me/whatsapp/confirm").header("Authorization", authorization)
                    .contentType("application/json").content("{\"token\":\"example\"}")).andExpect(status().is(expected));
            mvc.perform(delete("/clients/me/whatsapp").header("Authorization", authorization)).andExpect(status().is(expected));
        }
        verifyNoInteractions(accounts);
    }

    @Test
    void ownerComesFromSessionAndCannotBeSelectedInPayload() throws Exception {
        when(authenticatedUser.client()).thenReturn(com.example.demo.entities.Client.builder().id(17).build());
        when(accounts.confirm(17, "example")).thenReturn(new WhatsAppAccountService.Status(true, "573001234567", java.time.Instant.now()));
        mvc.perform(post("/clients/me/whatsapp/confirm").header("Authorization", "Bearer client-token")
                .contentType("application/json").content("{\"token\":\"example\",\"clientId\":99}"))
                .andExpect(status().isOk());
        verify(accounts).confirm(17, "example");
        mvc.perform(delete("/clients/me/whatsapp").header("Authorization", "Bearer client-token")).andExpect(status().isNoContent());
        verify(accounts).unlink(17);
    }

    @Test
    void anonymousVisitorsCannotReadCustomerPhoneNumbers() throws Exception {
        mvc.perform(get("/operators/whatsapp-requests")).andExpect(status().isUnauthorized());
        mvc.perform(get("/operators/whatsapp-requests/notifications")).andExpect(status().isUnauthorized());
        verifyNoInteractions(manager);
    }

    @Test
    void clientsCannotReadOrDismissOperatorNotifications() throws Exception {
        mvc.perform(get("/operators/whatsapp-requests").header("Authorization", "Bearer client-token"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/operators/whatsapp-requests/notifications").header("Authorization", "Bearer client-token"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/operators/whatsapp-requests/notifications/1/read").header("Authorization", "Bearer client-token"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/operators/whatsapp-requests/notifications/read-all").header("Authorization", "Bearer client-token"))
                .andExpect(status().isForbidden());
        mvc.perform(put("/operators/whatsapp-requests/573001234567/handled").param("requestedAt", "2026-11-23T12:00:00Z")
                .header("Authorization", "Bearer client-token")).andExpect(status().isForbidden());
        verifyNoInteractions(manager);
    }
}
