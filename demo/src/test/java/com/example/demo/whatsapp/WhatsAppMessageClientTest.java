package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WhatsAppMessageClientTest {
    @Test
    void sendsExpectedRequestWithBearerTokenAndParsesAcceptance() {
        var builder = RestClient.builder().baseUrl("https://graph.facebook.com");
        var server = MockRestServiceServer.bindTo(builder).build();
        var client = new WhatsAppMessageClient(new WhatsAppReplySettings(true, "fake-token", "123456", "v25.0"), builder.build());
        server.expect(requestTo("https://graph.facebook.com/v25.0/123456/messages"))
                .andExpect(method(HttpMethod.POST)).andExpect(header("Authorization", "Bearer fake-token"))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.messaging_product").value("whatsapp"))
                .andExpect(jsonPath("$.to").value("573001234567"))
                .andExpect(jsonPath("$.type").value("text"))
                .andExpect(jsonPath("$.text.body").value(WhatsAppMessageClient.TEST_REPLY))
                .andRespond(withSuccess("{\"messages\":[{\"id\":\"wamid.response\"}]}", MediaType.APPLICATION_JSON));
        assertThat(client.send("573001234567")).isEqualTo("wamid.response");
        server.verify();
    }

    @Test
    void enabledSendingRequiresCredentialsAndSettingsNeverPrintToken() {
        assertThatThrownBy(() -> new WhatsAppReplySettings(true, "", "", "v25.0")).isInstanceOf(IllegalArgumentException.class);
        assertThat(new WhatsAppReplySettings(false, "secret-test", "", "v25.0").toString()).doesNotContain("secret-test");
    }
}
