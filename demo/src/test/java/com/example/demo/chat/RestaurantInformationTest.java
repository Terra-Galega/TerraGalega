package com.example.demo.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.test.util.ReflectionTestUtils;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;

class RestaurantInformationTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(RestaurantInformation.class);

    @Test
    void defaultsContainConfirmedDetailsWithoutEnvironmentSetup() {
        context.run(ctx -> {
            String info = ctx.getBean(RestaurantInformation.class).assistantContext();
            assertThat(info).contains("Ak 15 # 92-81, Chapinero, Bogotá", "Lunes a sábado: 12:00 a 22:00",
                    "Domingo: 12:00 a 20:00", "America/Bogota", "+57 310 556 3040", "terragalega0@gmail.com");
            assertThat(info).doesNotContain("601 456 7890", "contacto@terragalega.co");
        });
    }

    @Test
    void environmentOverridesDefaultsAndBlankValuesRemainUnknown() {
        context.withPropertyValues("RESTAURANT_ADDRESS=Dirección nueva", "RESTAURANT_OPENING_HOURS=Martes: 14:00 a 18:00",
                "RESTAURANT_PHONE=+57 300 000 0000", "RESTAURANT_EMAIL=").run(ctx -> {
            String info = ctx.getBean(RestaurantInformation.class).assistantContext();
            assertThat(info).contains("Dirección nueva", "Martes: 14:00 a 18:00", "+57 300 000 0000", "Correo de contacto: No confirmado");
            assertThat(info).doesNotContain("92-81", "22:00", "terragalega0@gmail.com");
        });
    }

    @Test
    void webChatReceivesSameConfigurableInformationAsWhatsApp() {
        var gemini = mock(GeminiClient.class);
        var tools = mock(ChatTools.class);
        var info = new RestaurantInformation("Dirección configurada", "Domingo: 12:00 a 20:00", "+57 310 556 3040", "terragalega0@gmail.com");
        when(tools.menuText()).thenReturn("Menú de prueba");
        var response = mock(GenerateContentResponse.class);
        when(response.text()).thenReturn("Abrimos el domingo hasta las 20:00.");
        when(gemini.generate(anyList(), any())).thenReturn(response);
        var service = new ChatService();
        ReflectionTestUtils.setField(service, "geminiClient", gemini);
        ReflectionTestUtils.setField(service, "chatTools", tools);
        ReflectionTestUtils.setField(service, "restaurantInformation", info);
        var request = new ChatRequestDTO();
        request.setMessage("¿A qué hora cierran el domingo?");
        service.chat(1, request);
        var config = ArgumentCaptor.forClass(GenerateContentConfig.class);
        verify(gemini).generate(anyList(), config.capture());
        String prompt = config.getValue().systemInstruction().orElseThrow().parts().orElseThrow().getFirst().text().orElseThrow();
        assertThat(prompt).contains(info.assistantContext(), "Dirección configurada", "Domingo: 12:00 a 20:00");
    }
}
