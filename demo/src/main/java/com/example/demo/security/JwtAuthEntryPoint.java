package com.example.demo.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Responde 401 cuando alguien sin token, o con un token vencido o inválido,
// intenta entrar a un endpoint que exige iniciar sesión.
// El JSON tiene la misma forma que ApiError para que el front lo muestre igual
// que los demás errores. Se escribe a mano porque esto ocurre en los filtros,
// antes de llegar al GlobalExceptionHandler.
@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":401,\"error\":\"Unauthorized\","
                + "\"message\":\"Debes iniciar sesión para acceder a este recurso.\","
                + "\"path\":\"" + request.getRequestURI() + "\","
                + "\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
