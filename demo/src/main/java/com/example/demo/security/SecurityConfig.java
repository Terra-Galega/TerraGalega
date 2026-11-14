package com.example.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

// @Configuration: en esta clase se crean beans.
// @EnableWebSecurity: aquí se configura toda la seguridad web.
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // Cadena de filtros por la que pasa cada petición antes de llegar al controlador.
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CSRF solo aplica a apps con front y back en el mismo nodo (Thymeleaf
                // con sesión). Con Angular + API REST se puede desactivar.
                .csrf(AbstractHttpConfigurer::disable)
                // Usa el bean corsConfigurationSource de CorsConfig
                .cors(Customizer.withDefaults())
                // Necesario para que la consola de H2 (usa iframes) cargue bien
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()))
                // No usamos el formulario de login ni el popup de Spring
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // El servidor no guarda sesión: más adelante cada petición traerá su JWT
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // Mientras configuramos, todo público (como recomienda el profe)
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    // Bean para cifrar contraseñas con BCrypt. Se inyecta donde se necesite.
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}