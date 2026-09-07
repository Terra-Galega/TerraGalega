package com.example.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

// Clase de configuración de Spring encargada de registrar
// y definir dónde se debe aplicar el interceptor
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // Obtiene la instancia del interceptor que Spring creó
    // para poder registrarlo en la configuración
    @Autowired
    private SessionOwnershipInterceptor sessionOwnershipInterceptor;

    // Registra el SessionOwnershipInterceptor y define las rutas
    // en las que debe ejecutarse

    // El ** significa que también incluye las rutas que estén
    // dentro de esas secciones

    @Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(sessionOwnershipInterceptor)
                .addPathPatterns(
                        "/account/**",
                        "/clients/**",
                        "/admin/**");
        // El ** significa que también incluye las rutas que estén
        // dentro de esas secciones
    }
}