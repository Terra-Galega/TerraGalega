package com.example.demo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(
                        "http://localhost:8100", // ionic serve
                        "http://localhost", // Capacitor Android
                        "https://localhost",
                        "capacitor://localhost", // Capacitor iOS
                        "ionic://localhost")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
    }
}