package com.example.demo.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

// Crea y valida los JWT de la aplicación.
// Un JWT tiene tres partes separadas por puntos: encabezado, datos y firma.
// En los datos guardamos el correo del usuario, sus roles, la fecha de creación
// y la de vencimiento. Esos datos se pueden leer, pero no modificar: la firma se
// calcula con la clave secreta del servidor y deja de coincidir si alguien cambia algo.
@Component
public class JwtGenerator {

    private final SecretKey key;
    private final long expirationMs;

    // La clave y la duración se configuran en application.properties
    public JwtGenerator(@Value("${terra.jwt.secret}") String secret,
            @Value("${terra.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    // Genera el token después de un login correcto
    public String generateToken(Authentication authentication) {
        // Spring Security agrega la autoridad FACTOR_PASSWORD al entrar con contraseña.
        // No es un rol de la aplicación, así que no se incluye en el token.
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("FACTOR_"))
                .toList();
        return generateToken(authentication.getName(), roles);
    }

    // Genera el token a partir del correo y los roles.
    // Se usa, por ejemplo, cuando el cliente cambia su correo y necesita un token nuevo.
    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiration)
                .signWith(key)
                .compact();
    }

    // Devuelve el correo guardado en el token
    public String extractUsername(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    // Comprueba que la firma sea correcta y que el token no haya vencido
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
