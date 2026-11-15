package com.example.demo.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;

// Prueba manual: hace UNA llamada real a Gemini para comprobar que la key y el SDK funcionan.
class GeminiConnectionTest {

    // Usa el mismo modelo que te funcionó en Colab
    private static final String MODEL = "gemini-3.8-flash";

    @Test
    void geminiResponde() throws IOException {
        String apiKey = readApiKey();
        assumeTrue(apiKey != null && !apiKey.isBlank(),
                "Falta GEMINI_API_KEY (variable de entorno o archivo .env)");

        Client client = Client.builder().apiKey(apiKey).build();
        GenerateContentResponse response = client.models.generateContent(
                MODEL, "Responde en una frase: ¿qué plato típico gallego recomiendas?", null);

        System.out.println("Respuesta de Gemini: " + response.text());
        assertTrue(!response.text().isBlank());
    }

    // Busca primero la variable de entorno; si no existe, lee el archivo .env
    private static String readApiKey() throws IOException {
        String fromEnv = System.getenv("GEMINI_API_KEY");
        if (fromEnv != null && !fromEnv.isBlank()) {
            return fromEnv.trim();
        }
        Path envFile = Path.of(".env");
        if (!Files.exists(envFile)) {
            return null;
        }
        Properties props = new Properties();
        try (var reader = Files.newBufferedReader(envFile)) {
            props.load(reader);
        }
        String value = props.getProperty("GEMINI_API_KEY");
        return value == null ? null : value.trim();
    }
}