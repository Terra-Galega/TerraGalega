package com.example.demo.whatsapp;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
import tools.jackson.databind.ObjectMapper;
import com.example.demo.chat.RestaurantInformation;

class WhatsAppMenuTest {
    WhatsAppMenu menu = new WhatsAppMenu(new RestaurantInformation("Bogotá", "Domingo 12:00 a 20:00", "+57 310 556 3040", "terragalega0@gmail.com"), "https://terra.example/");
    ObjectMapper mapper = new ObjectMapper();

    @Test void onlySpecificCartConfirmationCommandsAreAccepted() {
        for (var command : java.util.Map.of("mi carrito", "CART", "confirmar carrito", "CART_CONFIRM", "cancelar cambio", "CART_CANCEL").entrySet()) {
            assertThat(menu.selection(mapper.valueToTree(java.util.Map.of("type", "text", "text", java.util.Map.of("body", command.getKey())))).kind()).isEqualTo(command.getValue());
        }
        for (String text : new String[]{"sí", "no confirmar carrito", "no quiero quitar nada"}) {
            assertThat(menu.selection(mapper.valueToTree(java.util.Map.of("type", "text", "text", java.util.Map.of("body", text)))).kind()).isEqualTo("AI");
        }
        var button = mapper.valueToTree(java.util.Map.of("type", "interactive", "interactive", java.util.Map.of("type", "button_reply", "button_reply", java.util.Map.of("id", "tg_cart_confirm:" + "a".repeat(64)))));
        assertThat(menu.selection(button).kind()).isEqualTo("CART_CONFIRM");
        assertThat(menu.selection(button).text()).isEqualTo("a".repeat(64));
    }

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
    void linkingIsExplicitAndEveryNativeMenuOptionHasAnEmojiWithinMetaLimits() {
        var selected = menu.selection(mapper.readTree("{\"type\":\"interactive\",\"interactive\":{\"type\":\"list_reply\",\"list_reply\":{\"id\":\"tg_link\"}}}"));
        assertThat(selected.kind()).isEqualTo("LINK");
        assertThat(menu.selection(mapper.readTree("{\"type\":\"text\",\"text\":{\"body\":\"Vincular mi cuenta\"}}" )).kind()).isEqualTo("LINK");
        assertThat(menu.selection(mapper.readTree("{\"type\":\"text\",\"text\":{\"body\":\"No quiero vincular mi cuenta\"}}" )).kind()).isEqualTo("AI");
        var rows = mapper.valueToTree(WhatsAppMenu.interactive("Hola")).path("action").path("sections").path(0).path("rows");
        assertThat(rows.size()).isEqualTo(7);
        for (var row : rows) {
            String title = row.path("title").asText();
            assertThat(title.length()).isLessThanOrEqualTo(24);
            assertThat(Character.isLetterOrDigit(title.codePointAt(0))).isFalse();
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
