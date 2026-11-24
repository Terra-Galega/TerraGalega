package com.example.demo.whatsapp;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import com.example.demo.chat.ChatTools;
import com.example.demo.chat.GeminiClient;
import com.example.demo.chat.RestaurantInformation;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.FunctionCall;

class WhatsAppAssistantTest {
    GeminiClient gemini = mock(GeminiClient.class);
    ChatTools catalog = mock(ChatTools.class);
    RestaurantInformation information = new RestaurantInformation("Ak 15 # 92-81, Chapinero, Bogotá", "Lunes a sábado: 12:00 a 22:00. Domingo: 12:00 a 20:00.", "+57 310 556 3040", "terragalega0@gmail.com");
    WhatsAppAssistant assistant = new WhatsAppAssistant(gemini, catalog, true, "https://terra.example", information);

    @Test
    void sendsMenuAndConversationToGeminiWithoutAnyBusinessTools() {
        when(catalog.menuText()).thenReturn("Pulpo | 45000 COP");
        var response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("Te recomiendo el pulpo por 45000 COP.");
        when(gemini.generate(anyList(), any())).thenReturn(response);
        assertThat(assistant.answer("¿Qué recomiendas?", List.of(new WhatsAppAssistant.Turn("Me gusta el marisco", "Tenemos pulpo"))))
                .contains("pulpo");
        var config = ArgumentCaptor.forClass(GenerateContentConfig.class);
        verify(gemini).generate(argThat(contents -> contents.size() == 3), config.capture());
        assertThat(config.getValue().tools()).isEmpty();
        String prompt = config.getValue().systemInstruction().orElseThrow().parts().orElseThrow().getFirst().text().orElseThrow();
        assertThat(prompt).contains("Pulpo | 45000 COP", "https://terra.example", "No puedes crear", "No pidas contraseñas", "Ak 15 # 92-81", "Domingo: 12:00 a 20:00", "+57 310 556 3040", "terragalega0@gmail.com", "No hay horario especial de festivos confirmado");
        verify(catalog).menuText();
        verifyNoMoreInteractions(catalog);
    }

    @Test
    void blankAndOversizedMessagesDoNotSpendTokens() {
        assertThat(assistant.answer(" ", List.of())).contains("consulta");
        assertThat(assistant.answer("x".repeat(501), List.of())).contains("500");
        verifyNoInteractions(gemini, catalog);
    }

    @Test
    void providerFailureReturnsUsefulFallback() {
        when(gemini.generate(anyList(), any())).thenThrow(new IllegalStateException("provider down"));
        assertThat(assistant.answer("Hola", List.of())).isEqualTo(WhatsAppAssistant.UNAVAILABLE);
    }

    @Test
    void unexpectedFunctionCallIsNeverExecuted() {
        var response = mock(GenerateContentResponse.class);
        when(response.functionCalls()).thenReturn(com.google.common.collect.ImmutableList.of(FunctionCall.builder().name("crear_reserva").build()));
        when(gemini.generate(anyList(), any())).thenReturn(response);
        assertThat(assistant.answer("Crea una reserva", List.of())).isEqualTo(WhatsAppAssistant.UNAVAILABLE);
        verify(catalog).menuText();
        verifyNoMoreInteractions(catalog);
    }

    @Test
    void limitsOutputAndCanReturnToFixedReply() {
        var response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("a".repeat(5000));
        when(gemini.generate(anyList(), any())).thenReturn(response);
        assertThat(assistant.answer("Hola", List.of())).hasSizeLessThanOrEqualTo(1500);
        var disabled = new WhatsAppAssistant(gemini, catalog, false, "http://localhost:4200", information);
        assertThat(disabled.answer("Hola", List.of())).isEqualTo(WhatsAppMessageClient.TEST_REPLY);
    }
}
