package com.example.demo.chat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.errors.ApiException;
import com.example.demo.service.ReservationRules;
import com.google.genai.types.Content;
import com.google.genai.types.FunctionCall;
import com.google.genai.types.FunctionResponse;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private static final int MAX_MESSAGE_LENGTH = 500;
    private static final int MAX_HISTORY_TEXT = 2000;
    private static final int MAX_HISTORY = 20;
    // Vueltas máximas de "Gemini pide función -> el back la ejecuta" por mensaje
    private static final int MAX_TOOL_ROUNDS = 5;
    private static final ZoneId ZONE = ZoneId.of("America/Bogota");

    @Autowired
    private GeminiClient geminiClient;

    @Autowired
    private ChatTools chatTools;

    public ChatResponseDTO chat(Integer clientId, ChatRequestDTO request) {
        String message = request.getMessage() == null ? "" : request.getMessage().trim();
        if (message.isEmpty()) {
            throw ApiException.badRequest("Escribe un mensaje.");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw ApiException.badRequest("El mensaje es demasiado largo (máximo 500 caracteres).");
        }

        List<Content> contents = new ArrayList<>();
        for (ChatMessageDTO m : cleanHistory(request.getHistory())) {
            contents.add(GeminiClient.content(m.getRole(), m.getText()));
        }
        contents.add(GeminiClient.content("user", message));

        GenerateContentConfig config = GenerateContentConfig.builder()
                .systemInstruction(Content.fromParts(Part.fromText(systemPrompt())))
                .tools(chatTools.tool())
                .temperature(0.3f)
                .maxOutputTokens(1024)
                .build();

        ChatTools.Effects effects = new ChatTools.Effects();

        for (int round = 0; round < MAX_TOOL_ROUNDS; round++) {
            GenerateContentResponse response = geminiClient.generate(contents, config);
            List<FunctionCall> calls = response.functionCalls();

            // Sin llamadas a funciones: es la respuesta final para el cliente
            if (calls == null || calls.isEmpty()) {
                String text = response.text();
                if (text == null || text.isBlank()) {
                    throw ApiException.serviceUnavailable("No pude generar una respuesta, intenta de nuevo.");
                }
                return new ChatResponseDTO(text.trim(), effects.cartChanged, effects.reservation);
            }

            // Se devuelve el turno del modelo tal cual (lleva las "thought signatures" de Gemini 3)
            Content modelTurn = response.candidates()
                    .filter(c -> !c.isEmpty())
                    .flatMap(c -> c.get(0).content())
                    .orElseThrow(() -> ApiException.serviceUnavailable("No pude generar una respuesta, intenta de nuevo."));
            contents.add(modelTurn);

            List<Part> results = new ArrayList<>();
            for (FunctionCall call : calls) {
                String name = call.name().orElse("");
                Map<String, Object> args = call.args().orElse(Map.of());
                log.info("Chat cliente {}: {} {}", clientId, name, args);

                Map<String, Object> result = chatTools.execute(name, args, clientId, effects);

                FunctionResponse.Builder fr = FunctionResponse.builder().name(name).response(result);
                call.id().ifPresent(fr::id);
                results.add(Part.builder().functionResponse(fr.build()).build());
            }
            contents.add(Content.builder().role("user").parts(results).build());
        }

        log.warn("Chat cliente {}: se superaron {} vueltas de funciones", clientId, MAX_TOOL_ROUNDS);
        return new ChatResponseDTO("Lo siento, no pude completar tu solicitud. ¿Puedes intentarlo de nuevo?",
                effects.cartChanged, effects.reservation);
    }

    private List<ChatMessageDTO> cleanHistory(List<ChatMessageDTO> raw) {
        List<ChatMessageDTO> clean = new ArrayList<>();
        if (raw == null) {
            return clean;
        }
        for (ChatMessageDTO m : raw) {
            boolean validRole = "user".equals(m.getRole()) || "model".equals(m.getRole());
            boolean validText = m.getText() != null && !m.getText().isBlank();
            if (validRole && validText) {
                String text = m.getText().trim();
                if (text.length() > MAX_HISTORY_TEXT) {
                    text = text.substring(0, MAX_HISTORY_TEXT);
                }
                clean.add(new ChatMessageDTO(m.getRole(), text));
            }
        }
        clean = new ArrayList<>(clean.subList(Math.max(0, clean.size() - MAX_HISTORY), clean.size()));
        while (!clean.isEmpty() && "model".equals(clean.get(0).getRole())) {
            clean.remove(0);
        }
        return clean;
    }

    private String systemPrompt() {
        LocalDate today = LocalDate.now(ZONE);
        String dayName = today.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.forLanguageTag("es-CO"));
        String slots = ReservationRules.SLOTS.stream().map(LocalTime::toString).collect(Collectors.joining(", "));
        String deposit = String.format(Locale.forLanguageTag("es-CO"), "$%,.0f", ReservationRules.DEPOSIT_AMOUNT);

        return """
                Eres Galleguito, el asistente virtual de TerraGalega, un restaurante de comida gallega.
                Si te preguntan tu nombre, di que eres Galleguito.
                Hablas siempre en español, con tono amable y respuestas breves (máximo 3 o 4 frases).
                Solo ayudas con el restaurante: menú, recomendaciones, carrito y reservas.
                Si te preguntan otra cosa, responde con amabilidad que solo puedes ayudar con el restaurante.
                No inventes platos, precios ni horarios: usa solo el menú y los datos de abajo.
                Nunca muestres al cliente los id de los platos.

                RECOMENDACIONES
                - Recomienda solo platos del menú. Si el cliente da gustos o restricciones (vegetariano,
                  sin mariscos, sin gluten, sin frutos secos, picante), respétalos usando las etiquetas.
                - Si no da detalles, recomienda los favoritos de la casa.
                - Usa agregar_al_carrito solo cuando el cliente lo pida o acepte la recomendación.
                  Luego confirma qué agregaste y el total del carrito.

                RESERVAS
                - Necesitas fecha, hora y número de personas (1 a 20). Si falta algo, pregúntalo.
                - Turnos válidos: %s. Si pide otra hora, ofrece el turno más cercano.
                - Convierte fechas como "mañana" o "el viernes" a AAAA-MM-DD usando la fecha de hoy.
                - Primero usa consultar_disponibilidad. Si hay mesa, resume fecha, hora, personas y mesa,
                  avisa que se paga un anticipo de %s NO reembolsable y pregunta si confirma.
                - Llama a crear_reserva solo después de que el cliente confirme explícitamente, con
                  anticipoAceptado = true. Si dio un motivo (cumpleaños, grado...) pásalo en tematica;
                  si dio un nombre para la reserva, en nombre.
                - Al crearla, dile que pague el anticipo con el botón "Pagar anticipo" en los próximos
                  30 minutos o la reserva se cancela.
                - Cada cliente tiene máximo una reserva. Para editarla o cancelarla, que vaya a la página de Reservas.

                Si una función responde con "error", explícale el problema al cliente con tus palabras.

                Hoy es %s %s (zona horaria de Bogotá).

                MENÚ (id | plato | precio | categoría | etiquetas | descripción):
                %s
                """.formatted(slots, deposit, dayName, today, chatTools.menuText());
    }
}