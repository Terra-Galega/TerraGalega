package com.example.demo.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// Filtro que se ejecuta una vez en cada petición, antes de llegar al controlador.
// Si la petición trae un JWT válido, deja al usuario autenticado en el
// SecurityContextHolder. Si no trae token, la petición sigue como anónima y son
// las reglas de SecurityConfig las que deciden si puede pasar.
// No se marca como @Component para que Spring no lo registre dos veces;
// lo crea SecurityConfig.
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtGenerator jwtGenerator;
    private final CustomUserDetailsService userDetailsService;

    public JwtAuthenticationFilter(JwtGenerator jwtGenerator, CustomUserDetailsService userDetailsService) {
        this.jwtGenerator = jwtGenerator;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String token = getJWT(request);

        if (token != null && jwtGenerator.validateToken(token)) {
            String username = jwtGenerator.extractUsername(token);
            try {
                // Se consulta la base de datos para tener los roles actuales
                // y saber si la cuenta sigue activa
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (userDetails.isEnabled()) {
                    // Las credenciales van en null porque el usuario ya se identificó con el token
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // A partir de aquí, cualquier parte del código puede saber quién hizo la petición
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (UsernameNotFoundException e) {
                // La cuenta fue eliminada: la petición sigue sin autenticar
            }
        }

        filterChain.doFilter(request, response);
    }

    // El token llega en el header "Authorization: Bearer <token>"
    private String getJWT(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
