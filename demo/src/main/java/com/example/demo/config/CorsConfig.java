package com.example.demo.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

// Define qué clientes (orígenes) pueden consumir la API, qué headers pueden
// enviar y qué métodos están permitidos. Spring Security usa este bean.
// Al añadir aquí las cabeceras CORS, los @CrossOrigin de los controladores
// ya no restringen más (Spring no vuelve a procesar CORS si ya están).
@Configuration
public class CorsConfig {

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            // La web abierta por la IP del PC (p. ej. desde el enlace del correo en el celular)
            @Value("${terra.email-link-url:}") String lanFrontendUrl) {

        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = new ArrayList<>(List.of(
                "http://localhost:4200", // Angular
                "http://localhost", // Capacitor Android
                "https://localhost",
                "capacitor://localhost" // Capacitor iOS
        ));
        if (!lanFrontendUrl.isBlank() && !origins.contains(lanFrontendUrl.trim())) {
            origins.add(lanFrontendUrl.trim());
        }
        config.setAllowedOrigins(origins);

        // Authorization es el header por donde viajará el JWT
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
