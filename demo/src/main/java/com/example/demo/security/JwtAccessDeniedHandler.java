package com.example.demo.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Responde 403 cuando el usuario sí inició sesión, pero su rol no tiene permiso
// para ese endpoint (por ejemplo, un cliente intentando entrar a algo del admin).
// Usa el mismo formato JSON que ApiError.
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"status\":403,\"error\":\"Forbidden\","
                + "\"message\":\"No tienes permiso para realizar esta acción.\","
                + "\"path\":\"" + request.getRequestURI() + "\","
                + "\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
