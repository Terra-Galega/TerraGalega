package com.example.demo.chat;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.errors.ApiException;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.HttpOptions;
import com.google.genai.types.Part;

// Hace las llamadas a Gemini (con modelo de respaldo si el principal está saturado).
@Service
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    @Value("${gemini.api-key:}")
    private String apiKey;

    @Value("${gemini.model}")
    private String model;

    @Value("${gemini.fallback-model:}")
    private String fallbackModel;

    private Client client;

    private synchronized Client getClient() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GEMINI_API_KEY no está configurada: revisa el archivo .env y desde qué carpeta arrancas el backend");
            throw ApiException.serviceUnavailable("El asistente no está disponible en este momento.");
        }
        if (client == null) {
            client = Client.builder()
                    .apiKey(apiKey.trim())
                    .httpOptions(HttpOptions.builder().timeout(30_000).build())
                    .build();
        }
        return client;
    }

    // Una vuelta de la conversación. La respuesta puede traer texto o llamadas a funciones.
    public GenerateContentResponse generate(List<Content> contents, GenerateContentConfig config) {
        try {
            return call(model, contents, config);
        } catch (ApiException e) {
            throw e;
        } catch (RuntimeException e) {
            if (isOverloaded(e) && fallbackModel != null && !fallbackModel.isBlank()) {
                log.warn("El modelo {} está saturado, probando con {}", model, fallbackModel);
                try {
                    return call(fallbackModel, contents, config);
                } catch (ApiException e2) {
                    throw e2;
                } catch (RuntimeException e2) {
                    throw toApiException(e2);
                }
            }
            throw toApiException(e);
        }
    }

    private GenerateContentResponse call(String modelName, List<Content> contents, GenerateContentConfig config) {
        long start = System.currentTimeMillis();
        try {
            GenerateContentResponse response = getClient().models.generateContent(modelName, contents, config);
            log.info("Gemini ({}) respondió en {} ms", modelName, System.currentTimeMillis() - start);
            return response;
        } catch (RuntimeException e) {
            log.warn("Gemini ({}) falló tras {} ms: {}", modelName, System.currentTimeMillis() - start, e.toString());
            throw e;
        }
    }

    private static boolean isOverloaded(RuntimeException e) {
        String detail = String.valueOf(e.getMessage());
        return detail.contains("503") || detail.contains("high demand") || detail.contains("UNAVAILABLE")
                || detail.contains("429") || detail.contains("RESOURCE_EXHAUSTED");
    }

    private static ApiException toApiException(RuntimeException e) {
        String detail = String.valueOf(e.getMessage());
        if (detail.contains("429") || detail.contains("RESOURCE_EXHAUSTED")) {
            return ApiException.serviceUnavailable("El asistente está muy ocupado. Intenta de nuevo en un minuto.");
        }
        return ApiException.serviceUnavailable("El asistente no está disponible en este momento.");
    }

    public static Content content(String role, String text) {
        return Content.builder().role(role).parts(List.of(Part.fromText(text))).build();
    }
}