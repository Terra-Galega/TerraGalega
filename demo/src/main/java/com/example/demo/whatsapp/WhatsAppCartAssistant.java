package com.example.demo.whatsapp;

import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.example.demo.chat.GeminiClient;
import com.example.demo.errors.ApiException;
import com.google.genai.types.*;

/** Gemini solo consulta y prepara propuestas; nunca ejecuta una mutación del carrito. */
@Service
public class WhatsAppCartAssistant {
    private final GeminiClient gemini;
    private final WhatsAppAssistant publicAssistant;
    private final WhatsAppCartService cart;
    private final boolean enabled;

    public WhatsAppCartAssistant(GeminiClient gemini, WhatsAppAssistant publicAssistant, WhatsAppCartService cart,
            @Value("${WHATSAPP_AI_ENABLED:true}") boolean enabled) {
        this.gemini = gemini; this.publicAssistant = publicAssistant; this.cart = cart; this.enabled = enabled;
    }

    WhatsAppCartService.Reply answer(WhatsAppCartService.Context context, String message, List<WhatsAppAssistant.Turn> history) {
        if (!enabled) return WhatsAppCartService.Reply.text(WhatsAppMessageClient.TEST_REPLY);
        if (message == null || message.isBlank() || message.length() > WhatsAppAssistant.MAX_INPUT) {
            return WhatsAppCartService.Reply.text("Escribe tu consulta en un mensaje de máximo 500 caracteres.");
        }
        try {
            var contents = new ArrayList<Content>();
            for (var turn : history) {
                contents.add(GeminiClient.content("user", turn.user()));
                contents.add(GeminiClient.content("model", turn.assistant()));
            }
            contents.add(GeminiClient.content("user", message));
            var config = GenerateContentConfig.builder().systemInstruction(Content.fromParts(Part.fromText(publicAssistant.cartPrompt())))
                    .tools(tools()).temperature(0.2f).maxOutputTokens(900).build();
            String lastCart = null;
            for (int round = 0; round < 3; round++) {
                var response = gemini.generate(contents, config);
                if (response == null) break;
                var calls = response.functionCalls();
                if (calls == null || calls.isEmpty()) {
                    String text = lastCart != null ? lastCart : response.text();
                    return WhatsAppCartService.Reply.text(text == null || text.isBlank() ? WhatsAppAssistant.UNAVAILABLE : limit(text));
                }
                // Un pedido compuesto se aclara antes de guardar propuestas, sin efectos parciales.
                if (calls.size() > 4 || calls.stream().anyMatch(c -> !List.of("ver_carrito", "preparar_cambio_carrito").contains(c.name().orElse("")))) {
                    return WhatsAppCartService.Reply.text("Por WhatsApp puedo ayudarte con el carrito; las reservas, pedidos y pagos se gestionan en la web.");
                }
                if (calls.stream().filter(c -> "preparar_cambio_carrito".equals(c.name().orElse(""))).count() > 1) {
                    return WhatsAppCartService.Reply.text("Hagamos un cambio de carrito a la vez. ¿Qué plato quieres agregar, cambiar o quitar primero?");
                }
                var proposal = calls.stream().filter(c -> "preparar_cambio_carrito".equals(c.name().orElse(""))).findFirst();
                if (proposal.isPresent()) return cart.prepare(context, proposal.get().args().orElse(Map.of()));
                var modelTurn = response.candidates().filter(c -> !c.isEmpty()).flatMap(c -> c.getFirst().content())
                        .orElseThrow(() -> new IllegalStateException("Turno de herramienta incompleto"));
                contents.add(modelTurn);
                var results = new ArrayList<Part>();
                for (var call : calls) {
                    var result = cart.inspect(context);
                    lastCart = String.valueOf(result.get("resumen"));
                    var builder = FunctionResponse.builder().name("ver_carrito").response(result);
                    call.id().ifPresent(builder::id);
                    results.add(Part.builder().functionResponse(builder.build()).build());
                }
                contents.add(Content.builder().role("user").parts(results).build());
            }
            return WhatsAppCartService.Reply.text(lastCart == null ? WhatsAppAssistant.UNAVAILABLE : lastCart);
        } catch (ApiException failure) {
            return WhatsAppCartService.Reply.text(failure.getMessage());
        } catch (RuntimeException failure) {
            return WhatsAppCartService.Reply.text(WhatsAppAssistant.UNAVAILABLE);
        }
    }

    static Tool tools() {
        return Tool.builder().functionDeclarations(
                FunctionDeclaration.builder().name("ver_carrito").description("Lee el carrito de la cuenta vinculada y devuelve las referencias exactas de sus líneas. No lo modifica.").build(),
                FunctionDeclaration.builder().name("preparar_cambio_carrito")
                        .description("Propone UN cambio pedido por el cliente. No lo ejecuta: requiere Confirmar. Agregar crea un plato SIN adicionales. Cantidad cambia el total de una línea existente; quitar elimina esa línea. Si hay variantes ambiguas, pregunta antes.")
                        .parameters(Schema.builder().type(Type.Known.OBJECT).properties(Map.of(
                                "accion", Schema.builder().type(Type.Known.STRING).enum_(List.of("agregar", "cantidad", "quitar")).build(),
                                "productoId", Schema.builder().type(Type.Known.INTEGER).description("Solo agregar: ID de un plato del menú activo").build(),
                                "lineaId", Schema.builder().type(Type.Known.STRING).description("Solo cantidad/quitar: referencia exacta obtenida con ver_carrito").build(),
                                "cantidad", Schema.builder().type(Type.Known.INTEGER).description("Entero 1 a 99. Para cantidad es el nuevo TOTAL; para agregar es lo que se suma").build()))
                                .required(List.of("accion")).build()).build()).build();
    }

    private static String limit(String text) {
        if (text.length() <= WhatsAppAssistant.MAX_OUTPUT) return text;
        int end = WhatsAppAssistant.MAX_OUTPUT - 1;
        if (Character.isHighSurrogate(text.charAt(end - 1))) end--;
        return text.substring(0, end) + "…";
    }
}
