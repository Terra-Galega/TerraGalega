package com.example.demo.whatsapp;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import com.example.demo.chat.RestaurantInformation;

class WhatsAppMenuTest {
    WhatsAppMenu menu = new WhatsAppMenu(new RestaurantInformation("Bogotá", "Domingo 12:00 a 20:00", "+57 310 556 3040", "terragalega0@gmail.com"), "https://terra.example/");
    ObjectMapper mapper = new ObjectMapper();

    @Test
    void onlyExplicitCommandsChangeConversationFlow() {
        for (String greeting : new String[]{"Hola", "Menú", "opciones", "Hola, quiero conocer el menú de TerraGalega."}) {
            assertThat(menu.selection(mapper.valueToTree(java.util.Map.of("type", "text", "text", java.util.Map.of("body", greeting)))).kind()).isEqualTo("MENU");
        }
        for (String command : new String[]{"operador", "Quiero hablar con un operario", "Necesito comunicarme con una persona"}) {
            assertThat(menu.selection(mapper.valueToTree(java.util.Map.of("type", "text", "text", java.util.Map.of("body", command)))).kind()).isEqualTo("OPERATOR");
        }
        for (String text : new String[]{"No quiero hablar con un operador", "¿Qué hace un operador?", "Hola, ¿qué platos tienen?"}) {
            assertThat(menu.selection(mapper.valueToTree(java.util.Map.of("type", "text", "text", java.util.Map.of("body", text)))).kind()).isEqualTo("AI");
        }
    }

    @Test
    void fixedChoicesUsePublicDataAndWebsiteWithoutBusinessActions() {
        assertThat(menu.fixedAnswer("INFO")).contains("Bogotá", "Domingo 12:00 a 20:00");
        assertThat(menu.fixedAnswer("CATALOG")).contains("https://terra.example/menu");
        assertThat(menu.fixedAnswer("RESERVATIONS")).contains("https://terra.example/reservations");
        assertThat(menu.fixedAnswer("OPERATOR")).contains("Registramos tu solicitud", "puedes seguir");
        assertThat(menu.fixedAnswer("AI")).isNull();
    }
}
